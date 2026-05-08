package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.exercise.ExerciseResponse;
import com.molina.gainstrack.api.dto.routine.RoutineDetailResponse;
import com.molina.gainstrack.api.dto.routine.RoutineExerciseResponse;
import com.molina.gainstrack.api.dto.routine.RoutineSummaryResponse;
import com.molina.gainstrack.api.dto.session.SetResponse;
import com.molina.gainstrack.api.dto.shared.MuscleGroupResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Objects;

@Repository
public class RoutineRepository {

    private final JdbcClient jdbcClient;

    public RoutineRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<RoutineSummaryResponse> findAll(Long userId) {
        return jdbcClient.sql("SELECT id, name, created_at, notes " +
                              "FROM routines " +
                              "WHERE user_id = :userId")
                         .param("userId", userId)
                         .query(RoutineSummaryResponse.class)
                         .list();
    }

    public RoutineDetailResponse findById(Long id, Long userId) {
        return jdbcClient.sql("SELECT id AS routine_id, " +
                                     "name AS routine_name, " +
                                     "created_at AS routine_created_at, " +
                                     "notes AS routine_notes " +
                                     "FROM routines " +
                                     "WHERE id = :id AND user_id = :userId")
                         .param("id", id)
                         .param("userId", userId)
                         .query((rs, rowNum) -> {
                             List<RoutineExerciseResponse> routineExercises =
                                     jdbcClient.sql("SELECT re.id AS routine_exercise_id, " +
                                                           "re.order_index AS routine_exercise_order_index, " +
                                                           "re.notes AS routine_exercise_notes, " +
                                                           "e.id AS exercise_id, " +
                                                           "e.name AS exercise_name, " +
                                                           "mg.id AS muscle_group_id, " +
                                                           "mg.name AS muscle_group_name, " +
                                                           "e.user_id AS exercise_user_id, " +
                                                           "e.is_predefined AS exercise_is_predefined " +
                                                    "FROM routine_exercises re " +
                                                    "JOIN exercises e ON re.exercise_id = e.id " +
                                                    "JOIN muscle_groups mg ON e.muscle_group_id = mg.id " +
                                                    "WHERE re.routine_id = :routineId")
                                             .param("routineId", id)
                                             .query((rs2, rowNum2) -> {
                                                List<SetResponse> routineExerciseSets =
                                                        jdbcClient.sql("SELECT id, set_number, weight, reps, notes " +
                                                                        "FROM routine_sets " +
                                                                        "WHERE routine_exercise_id = :reId")
                                                                .param("reId", rs2.getLong("routine_exercise_id"))
                                                                .query((rs3, rowNum3) -> new SetResponse(
                                                                        rs3.getLong("id"),
                                                                        rs3.getInt("set_number"),
                                                                        rs3.getDouble("weight"),
                                                                        rs3.getInt("reps"),
                                                                        rs3.getString("notes")
                                                                ))
                                                                .list();

                                                return new RoutineExerciseResponse(
                                                        rs2.getLong("routine_exercise_id"),
                                                        rs2.getInt("routine_exercise_order_index"),
                                                        rs2.getString("routine_exercise_notes"),
                                                        new ExerciseResponse(
                                                                rs2.getLong("exercise_id"),
                                                                rs2.getString("exercise_name"),
                                                                new MuscleGroupResponse(
                                                                        rs2.getLong("muscle_group_id"),
                                                                        rs2.getString("muscle_group_name")
                                                                ),
                                                                rs2.getLong("exercise_user_id"),
                                                                rs2.getBoolean("exercise_is_predefined")
                                                        ),
                                                        routineExerciseSets
                                                );
                                             })
                                             .list();
                             return new RoutineDetailResponse(
                                     rs.getLong("routine_id"),
                                     rs.getString("routine_name"),
                                     rs.getDate("routine_created_at")
                                       .toLocalDate(),
                                     rs.getString("routine_notes"),
                                     routineExercises
                             );
                         })
                         .single();
    }

    public RoutineSummaryResponse save(Long userId, String name, String notes) {
        // Se crea rutina con datos básicos
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("INSERT INTO routines (user_id, name, notes) " +
                       "VALUES (:userId, :name, :notes)")
                  .param("userId", userId)
                  .param("name", name)
                  .param("notes", notes)
                  .update(keyHolder);
        Long routineId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        return jdbcClient.sql("SELECT id, name, created_at, notes " +
                              "FROM routines " +
                              "WHERE id = :routineId AND user_id = :userId")
                         .param("routineId", routineId)
                         .param("userId", userId)
                         .query(RoutineSummaryResponse.class)
                         .single();
    }

    public void update(Long id, Long userId, String name, String notes) {
        jdbcClient.sql("UPDATE routines " +
                       "SET name = :name, notes = :notes " +
                       "WHERE id = :id " +
                       "AND user_id = :userId")
                  .param("name", name)
                  .param("notes", notes)
                  .param("id", id)
                  .param("userId", userId)
                  .update();
    }
}

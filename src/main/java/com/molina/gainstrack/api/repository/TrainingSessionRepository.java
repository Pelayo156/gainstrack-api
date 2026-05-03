package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.exercise.ExerciseResponse;
import com.molina.gainstrack.api.dto.gym.GymResponse;
import com.molina.gainstrack.api.dto.routine.RoutineExerciseRow;
import com.molina.gainstrack.api.dto.session.*;
import com.molina.gainstrack.api.dto.shared.MuscleGroupResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * Repositorio de acceso a datos para la tabla training_sessions.
 * Ejecuta SQL puro mediante JdbcClient, sin ORM.
 * Todas las operaciones están acotadas al usuario propietario
 * para garantizar aislamiento de datos entre usuarios.
 */
@Repository
public class TrainingSessionRepository {

    /**
     * @param jdbcClient cliente JDBC para ejecutar consultas SQL
     */
    private final JdbcClient jdbcClient;

    public TrainingSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Retorna el resumen de todas las sesiones de entrenamiento del usuario.
     * Incluye los datos del gimnasio asociado mediante LEFT JOIN —
     * sesiones sin gym muestran "No especificado".
     * No incluye ejercicios ni sets — usar findById para el detalle completo.
     *
     * @param userId id del usuario autenticado
     * @return lista de sesiones con sus datos de cabecera y gimnasio
     */
    public List<TrainingSessionSummaryResponse> findAll(Long userId) {
        return jdbcClient.sql("SELECT ts.id AS training_session_id, " +
                                      "g.id AS gym_id, " +
                                      "g.name AS gym_name, " +
                                      "ts.session_date AS training_session_date, " +
                                      "ts.notes AS training_session_notes " +
                              "FROM training_sessions ts " +
                              "LEFT JOIN gyms g ON ts.gym_id = g.id " +
                              "WHERE ts.user_id = :userId")
                .param("userId", userId)
                .query((rs, rowNum) -> {
                    Long gymId = rs.getObject("gym_id", Long.class);
                    GymResponse gym = gymId != null
                            ? new GymResponse(gymId, rs.getString("gym_name"))
                            : new GymResponse(null, "No especificado");

                    return new TrainingSessionSummaryResponse(
                            rs.getLong("training_session_id"),
                            gym,
                            rs.getDate("training_session_date")
                                    .toLocalDate(),
                            rs.getString("training_session_notes")
                    );
                })
                .list();
    }

    /**
     * Crea una nueva sesión de entrenamiento ejecutando una rutina existente.
     * Copia automáticamente todos los ejercicios y sets de la rutina
     * como punto de partida para el usuario durante la sesión.
     * La fecha se asigna automáticamente con NOW() en MySQL.
     * Delega a findById para retornar el detalle completo de la sesión creada.
     *
     * @param userId    id del usuario autenticado
     * @param gymId     id del gimnasio donde se realiza la sesión — puede ser null
     * @param routineId id de la rutina a ejecutar — obligatorio
     * @param notes     título o descripción de la sesión ingresado por el usuario
     * @return TrainingSessionDetailResponse con la sesión completa incluyendo
     *         ejercicios y sets copiados desde la rutina
     */
    public TrainingSessionDetailResponse save(Long userId, Long gymId, Long routineId, String notes) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql("INSERT INTO training_sessions (user_id, routine_id, gym_id, session_date, notes) " +
                            "VALUES (:userId, :routineId, :gymId, NOW(), :notes)")
                       .param("userId", userId)
                       .param("routineId", routineId)
                       .param("gymId", gymId)
                       .param("notes", notes)
                       .update(keyHolder);
        Long sessionId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        // Se obtienen los ejercicios de la rutina que está asociada a la sesión
        List<RoutineExerciseRow> routineExercises =
                jdbcClient.sql("SELECT id, exercise_id, order_index, notes " +
                               "FROM routine_exercises " +
                               "WHERE routine_id = :routineId")
                          .param("routineId", routineId)
                          .query(RoutineExerciseRow.class)
                          .list();

        for (RoutineExerciseRow routineExercise : routineExercises) {
            // Inserto ejercicio dentro de session_exercises
            jdbcClient.sql("INSERT INTO session_exercises (session_id, exercise_id, order_index, notes) " +
                            "VALUES (:sessionId, :exerciseId, :orderIndex, :notes)")
                      .param("sessionId", sessionId)
                      .param("exerciseId", routineExercise.exerciseId())
                      .param("orderIndex", routineExercise.orderIndex())
                      .param("notes", routineExercise.notes())
                      .update(keyHolder);
            Long sessionExerciseId = keyHolder.getKey().longValue();

            // Se obtienen sets asociados a cada ejercicio de la rutina
            List<SetRow> routineExerciseSets =
                    jdbcClient.sql("SELECT id, set_number, weight, reps, notes " +
                                   "FROM routine_sets " +
                                   "WHERE routine_exercise_id = :routineExerciseId")
                              .param("routineExerciseId", routineExercise.id())
                              .query(SetRow.class)
                              .list();

            for (SetRow set : routineExerciseSets) {
                // se insertan sets de cada ejercicio de la rutina
                jdbcClient.sql("INSERT INTO sets (session_exercise_id, set_number, weight, reps, notes) " +
                               "VALUES (:sessionExerciseId, :setNumber, :weight, :reps, :notes)")
                          .param("sessionExerciseId", sessionExerciseId)
                          .param("setNumber", set.setNumber())
                          .param("weight", set.weight())
                          .param("reps", set.reps())
                          .param("notes", set.notes())
                          .update();
            }
        }

        // Query 1: Datos sesión recién creada
        return this.findById(sessionId, userId);
    }

    /**
     * Retorna el detalle completo de una sesión de entrenamiento.
     * Ejecuta tres queries anidadas para construir la respuesta:
     * 1. Datos de la sesión y gimnasio
     * 2. Ejercicios de la sesión con sus datos del catálogo
     * 3. Sets de cada ejercicio
     * El userId garantiza que el usuario solo pueda ver sus propias sesiones.
     *
     * @param id     id de la sesión a consultar
     * @param userId id del usuario propietario — previene acceso a sesiones ajenas
     * @return TrainingSessionDetailResponse con ejercicios y sets anidados
     */
    public TrainingSessionDetailResponse findById(Long id, Long userId) {
        return jdbcClient.sql("SELECT ts.id AS session_id, " +
                              "g.id AS gym_id, " +
                              "g.name AS gym_name, " +
                              "ts.session_date AS session_date, " +
                              "ts.notes AS session_notes " +
                              "FROM training_sessions ts " +
                              "LEFT JOIN gyms AS g ON ts.gym_id = g.id " +
                              "WHERE ts.id = :sessionId AND ts.user_id = :userId")
                .param("sessionId", id)
                .param("userId", userId)
                .query((rs, rowNum) -> {
                    Long gymIdQuery = rs.getObject("gym_id", Long.class);
                    GymResponse gym = gymIdQuery != null
                            ? new GymResponse(gymIdQuery, rs.getString("gym_name"))
                            : new GymResponse(null, "No especificado");

                    // Query 2: ejercicios de la sesión
                    List<SessionExerciseResponse> sessionExercises =
                            jdbcClient.sql("SELECT se.id AS session_exercise_id, " +
                                                  "se.order_index AS session_exercise_order_index, " +
                                                  "se.notes AS session_exercise_notes, " +
                                                  "e.id AS exercise_id, " +
                                                  "e.name AS exercise_name, " +
                                                  "mg.id AS muscle_group_id, " +
                                                  "mg.name AS muscle_group_name, " +
                                                  "e.user_id AS exercise_user_id, " +
                                                  "e.is_predefined AS exercise_is_predefined " +
                                                  "FROM session_exercises se " +
                                                  "JOIN exercises e ON se.exercise_id = e.id " +
                                                  "JOIN muscle_groups mg ON e.muscle_group_id = mg.id " +
                                                  "WHERE se.session_id = :sessionId")
                                    .param("sessionId", id)
                                    .query((rs2, rowNum2) -> {
                                        // Query 3: sets de cada ejercicio
                                        List<SetResponse> sessionExerciseSets =
                                                jdbcClient.sql("SELECT id, set_number, weight, reps, notes " +
                                                                "FROM sets " +
                                                                "WHERE session_exercise_id = :seId")
                                                        .param("seId", rs2.getLong("session_exercise_id"))
                                                        .query((rs3, rowNum3) -> new SetResponse(
                                                                rs3.getLong("id"),
                                                                rs3.getInt("set_number"),
                                                                rs3.getDouble("weight"),
                                                                rs3.getInt("reps"),
                                                                rs3.getString("notes")
                                                        ))
                                                        .list();

                                        return new SessionExerciseResponse(
                                                rs2.getLong("session_exercise_id"),
                                                rs2.getInt("session_exercise_order_index"),
                                                rs2.getString("session_exercise_notes"),
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
                                                sessionExerciseSets
                                        );
                                    })
                                    .list();
                    return new TrainingSessionDetailResponse(
                            rs.getLong("session_id"),
                            gym,
                            rs.getDate("session_date")
                              .toLocalDate(),
                            rs.getString("session_notes"),
                            sessionExercises
                    );
                })
                .single();
    }

    /**
     * Elimina una sesión de entrenamiento por su id.
     * El userId garantiza que el usuario solo pueda eliminar sus propias sesiones.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los session_exercises y sets asociados.
     *
     * @param id     id de la sesión a eliminar
     * @param userId id del usuario propietario — previene eliminación de sesiones ajenas
     */
    public void deleteById(Long id, Long userId) {
        jdbcClient.sql("DELETE FROM training_sessions " +
                       "WHERE id = :id AND user_id = :userId")
                  .param("id", id)
                  .param("userId", userId)
                  .update();
    }
}

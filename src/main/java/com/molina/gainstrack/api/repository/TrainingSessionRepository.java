package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.*;
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
     * Incluye los datos del gimnasio asociado a cada sesión mediante JOIN.
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
                .query((rs, rowNum) -> new TrainingSessionSummaryResponse(
                        rs.getLong("training_session_id"),
                        new GymResponse(
                                rs.getLong("gym_id"),
                                rs.getString("gym_name")
                        ),
                        rs.getDate("training_session_date")
                          .toLocalDate(),
                        rs.getString("training_session_notes")
                ))
                .list();
    }

    /**
     * Inserta una nueva sesión de entrenamiento y retorna su resumen.
     * Si routineId es null, crea la sesión vacía sin ejercicios precargados.
     * Si routineId tiene valor, crea la sesión y precarga los ejercicios
     * de la rutina en session_exercises (pendiente de implementar).
     * La fecha de sesión se asigna automáticamente con NOW() en MySQL.
     *
     * @param userId    id del usuario autenticado
     * @param gymId     id del gimnasio donde se realiza la sesión
     * @param routineId id de la rutina a precargar — puede ser null
     * @return TrainingSessionSummaryResponse con los datos de la sesión creada
     */
    public TrainingSessionDetailResponse save(Long userId, Long gymId, Long routineId) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql("INSERT INTO training_sessions (user_id, routine_id, gym_id, session_date) " +
                            "VALUES (:userId, :routineId, :gymId, NOW())")
                       .param("userId", userId)
                       .param("routineId", routineId)
                       .param("gymId", gymId)
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

        return jdbcClient.sql("");
    }

    /**
     * Elimina una sesión de entrenamiento por su id.
     * El userId garantiza que el usuario solo pueda eliminar sus propias sesiones.
     * Por el CASCADE del modelo relacional, se eliminarán también
     * todos los session_exercises y sets asociados a esta sesión.
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

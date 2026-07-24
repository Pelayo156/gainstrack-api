package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.exercise.ExerciseResponse;
import com.molina.gainstrack.api.dto.gym.GymResponse;
import com.molina.gainstrack.api.dto.session.*;
import com.molina.gainstrack.api.dto.shared.MuscleGroupResponse;
import com.molina.gainstrack.api.exception.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
                              "WHERE ts.user_id = :userId " +
                              "ORDER BY ts.session_date DESC")
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
     * Retorna el resumen de todas las sesiones asociadas a una rutina del usuario.
     * Incluye los datos del gimnasio mediante LEFT JOIN.
     * Ordenadas de más reciente a más antigua.
     *
     * @param routineId id de la rutina cuyas sesiones se consultan
     * @param userId    id del usuario propietario — previene acceso a sesiones ajenas
     * @return lista de sesiones ordenadas por fecha descendente
     */
    public List<TrainingSessionSummaryResponse> findAllByRoutineId(Long routineId,
                                                                   Long userId) {
        return jdbcClient.sql("SELECT ts.id AS training_session_id, " +
                              "g.id AS gym_id, " +
                              "g.name AS gym_name, " +
                              "ts.session_date AS training_session_date, " +
                              "ts.notes AS training_session_notes " +
                              "FROM training_sessions ts " +
                              "LEFT JOIN gyms g ON ts.gym_id = g.id " +
                              "WHERE ts.routine_id = :routineId AND ts.user_id = :userId " +
                              "ORDER BY ts.session_date DESC")
                .param("routineId", routineId)
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
     * El punto de partida de los ejercicios y sets depende del historial del usuario:
     * - Si existe una sesión previa del usuario para la misma rutina y el mismo
     *   gimnasio (gymId null incluido, representando "sin gimnasio"/sesión libre),
     *   se copian los ejercicios y sets reales de esa última sesión — permite
     *   continuar el progreso real registrado la última vez en ese gimnasio.
     * - Si es la primera vez que el usuario entrena esta rutina en ese gimnasio,
     *   se copian los ejercicios y sets de referencia de la plantilla de la rutina.
     * La fecha se asigna automáticamente con NOW() en MySQL.
     * Delega a findById para retornar el detalle completo de la sesión creada.
     *
     * @param userId    id del usuario autenticado
     * @param gymId     id del gimnasio donde se realiza la sesión — puede ser null
     * @param routineId id de la rutina a ejecutar — obligatorio
     * @param notes     título o descripción de la sesión ingresado por el usuario
     * @return TrainingSessionDetailResponse con la sesión completa incluyendo
     *         ejercicios y sets copiados desde la última sesión o desde la rutina
     */
    @Transactional
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

        Long lastSessionId = this.findLastSessionId(userId, routineId, gymId, sessionId);

        if (lastSessionId != null) {
            this.copyFromSession(sessionId, lastSessionId);
        } else {
            this.copyFromRoutineTemplate(sessionId, routineId);
        }

        // Query 1: Datos sesión recién creada
        return this.findById(sessionId, userId);
    }

    /**
     * Busca la sesión más reciente del usuario para una rutina y gimnasio específicos,
     * excluyendo la sesión recién creada. gymId puede ser null — en ese caso se
     * consideran únicamente sesiones sin gimnasio asociado (sesión libre de gimnasio),
     * tratando el valor null como un gimnasio más mediante el operador NULL-safe
     * <=> de MySQL para comparar gym_id.
     *
     * @param userId           id del usuario propietario
     * @param routineId        id de la rutina ejecutada
     * @param gymId            id del gimnasio elegido para la sesión — null representa
     *                         "sin gimnasio"
     * @param excludeSessionId id de la sesión recién creada — se excluye de la búsqueda
     * @return id de la última sesión encontrada, o null si no existe historial previo
     *         del usuario para esa rutina en ese gimnasio
     */
    private Long findLastSessionId(Long userId, Long routineId, Long gymId, Long excludeSessionId) {
        return this.jdbcClient.sql("SELECT id FROM training_sessions " +
                                   "WHERE user_id = :userId AND routine_id = :routineId " +
                                   "AND gym_id <=> :gymId AND id != :excludeSessionId " +
                                   "ORDER BY session_date DESC, id DESC " +
                                   "LIMIT 1")
                              .param("userId", userId)
                              .param("routineId", routineId)
                              .param("gymId", gymId)
                              .param("excludeSessionId", excludeSessionId)
                              .query(Long.class)
                              .optional()
                              .orElse(null);
    }

    /**
     * Copia ejercicios y sets desde una sesión previa hacia la sesión recién creada.
     * Se usa como punto de partida cuando el usuario ya entrenó esta rutina en el
     * gimnasio elegido — preserva los pesos y reps reales registrados la última vez,
     * en vez de los valores de referencia de la plantilla de la rutina.
     *
     * @param newSessionId  id de la sesión recién creada
     * @param lastSessionId id de la última sesión del usuario para la misma rutina y gimnasio
     */
    private void copyFromSession(Long newSessionId, Long lastSessionId) {
        List<ExerciseCopyRow> lastSessionExercises =
                jdbcClient.sql("SELECT id, exercise_id, order_index, notes " +
                               "FROM session_exercises " +
                               "WHERE session_id = :lastSessionId")
                          .param("lastSessionId", lastSessionId)
                          .query(ExerciseCopyRow.class)
                          .list();

        for (ExerciseCopyRow exercise : lastSessionExercises) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcClient.sql("INSERT INTO session_exercises (session_id, exercise_id, order_index, notes) " +
                            "VALUES (:sessionId, :exerciseId, :orderIndex, :notes)")
                      .param("sessionId", newSessionId)
                      .param("exerciseId", exercise.exerciseId())
                      .param("orderIndex", exercise.orderIndex())
                      .param("notes", exercise.notes())
                      .update(keyHolder);
            Long newSessionExerciseId = Objects.requireNonNull(keyHolder.getKey()).longValue();

            List<SetCopyRow> lastSessionSets =
                    jdbcClient.sql("SELECT set_number, weight, reps, notes " +
                                   "FROM sets " +
                                   "WHERE session_exercise_id = :sessionExerciseId")
                              .param("sessionExerciseId", exercise.id())
                              .query(SetCopyRow.class)
                              .list();

            for (SetCopyRow set : lastSessionSets) {
                jdbcClient.sql("INSERT INTO sets (session_exercise_id, set_number, weight, reps, notes) " +
                               "VALUES (:sessionExerciseId, :setNumber, :weight, :reps, :notes)")
                          .param("sessionExerciseId", newSessionExerciseId)
                          .param("setNumber", set.setNumber())
                          .param("weight", set.weight())
                          .param("reps", set.reps())
                          .param("notes", set.notes())
                          .update();
            }
        }
    }

    /**
     * Copia ejercicios y sets desde la plantilla de la rutina hacia la sesión recién creada.
     * Se usa cuando el usuario entrena esta rutina por primera vez en el gimnasio elegido
     * y no existe una sesión previa de la cual partir.
     *
     * @param newSessionId id de la sesión recién creada
     * @param routineId    id de la rutina cuya plantilla se copia
     */
    private void copyFromRoutineTemplate(Long newSessionId, Long routineId) {
        List<ExerciseCopyRow> routineExercises =
                jdbcClient.sql("SELECT id, exercise_id, order_index, notes " +
                               "FROM routine_exercises " +
                               "WHERE routine_id = :routineId")
                          .param("routineId", routineId)
                          .query(ExerciseCopyRow.class)
                          .list();

        for (ExerciseCopyRow routineExercise : routineExercises) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcClient.sql("INSERT INTO session_exercises (session_id, exercise_id, order_index, notes) " +
                            "VALUES (:sessionId, :exerciseId, :orderIndex, :notes)")
                      .param("sessionId", newSessionId)
                      .param("exerciseId", routineExercise.exerciseId())
                      .param("orderIndex", routineExercise.orderIndex())
                      .param("notes", routineExercise.notes())
                      .update(keyHolder);
            Long sessionExerciseId = Objects.requireNonNull(keyHolder.getKey()).longValue();

            List<SetCopyRow> routineExerciseSets =
                    jdbcClient.sql("SELECT set_number, weight, reps, notes " +
                                   "FROM routine_sets " +
                                   "WHERE routine_exercise_id = :routineExerciseId")
                              .param("routineExerciseId", routineExercise.id())
                              .query(SetCopyRow.class)
                              .list();

            for (SetCopyRow set : routineExerciseSets) {
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
    }

    /**
     * Retorna el resumen de la última sesión de entrenamiento del usuario para una
     * rutina y gimnasio específicos, ordenada por fecha descendente.
     * gymId puede ser null — se usa el operador NULL-safe <=> de MySQL para incluir
     * sesiones sin gimnasio asociado (sesión libre de gimnasio) como un grupo
     * válido de búsqueda, tratando null como un gimnasio más.
     * Incluye los datos del gimnasio asociado mediante LEFT JOIN.
     *
     * @param routineId id de la rutina consultada
     * @param gymId     id del gimnasio consultado — null representa sin gimnasio
     * @param userId    id del usuario propietario — previene acceso a sesiones ajenas
     * @return TrainingSessionSummaryResponse con el resumen de la última sesión encontrada
     * @throws NotFoundException si no existe una sesión previa para esa combinación
     */
    public TrainingSessionSummaryResponse findLastByRoutineAndGym(Long routineId, Long gymId, Long userId) {
        return jdbcClient.sql("SELECT ts.id AS training_session_id, " +
                              "g.id AS gym_id, " +
                              "g.name AS gym_name, " +
                              "ts.session_date AS training_session_date, " +
                              "ts.notes AS training_session_notes " +
                              "FROM training_sessions ts " +
                              "LEFT JOIN gyms g ON ts.gym_id = g.id " +
                              "WHERE ts.routine_id = :routineId AND ts.user_id = :userId " +
                              "AND ts.gym_id <=> :gymId " +
                              "ORDER BY ts.session_date DESC, ts.id DESC " +
                              "LIMIT 1")
                .param("routineId", routineId)
                .param("userId", userId)
                .param("gymId", gymId)
                .query((rs, rowNum) -> {
                    Long gymIdQuery = rs.getObject("gym_id", Long.class);
                    GymResponse gym = gymIdQuery != null
                            ? new GymResponse(gymIdQuery, rs.getString("gym_name"))
                            : new GymResponse(null, "No especificado");

                    return new TrainingSessionSummaryResponse(
                            rs.getLong("training_session_id"),
                            gym,
                            rs.getDate("training_session_date")
                                    .toLocalDate(),
                            rs.getString("training_session_notes")
                    );
                })
                .optional()
                .orElseThrow(() -> new NotFoundException(
                        "No existe una sesión previa para la rutina y gimnasio especificados"));
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
                    List<TrainingSessionExerciseResponse> sessionExercises =
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
                                           "WHERE se.session_id = :sessionId " +
                                           "ORDER BY se.order_index ASC")
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

                                        return new TrainingSessionExerciseResponse(
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
                .optional()
                .orElseThrow(() -> new NotFoundException("Sesión de entrenamiento no encontrada"));
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
        int affectedRows = jdbcClient.sql("DELETE FROM training_sessions " +
                                          "WHERE id = :id AND user_id = :userId")
                                     .param("id", id)
                                     .param("userId", userId)
                                     .update();

        if (affectedRows == 0) {
            throw new NotFoundException("Sesión de entrenamiento no encontrada");
        }
    }

    /**
     * Actualiza las notas de una sesión de entrenamiento.
     * Usa COALESCE para preservar el valor actual si notes llega null.
     * El userId garantiza que el usuario solo pueda editar sus propias sesiones.
     *
     * @param id     id de la sesión a actualizar
     * @param userId id del usuario propietario — previene edición de sesiones ajenas
     * @param notes  nuevas notas — puede ser null para mantener las actuales
     * @throws NotFoundException si la sesión no existe o no pertenece al usuario
     */
    public void update(Long id, Long userId, String notes) {
        int affectedRows = this.jdbcClient.sql("UPDATE training_sessions " +
                                               "SET notes = COALESCE(:notes, notes) " +
                                               "WHERE id = :id " +
                                               "AND user_id = :userId")
                                          .param("notes", notes)
                                          .param("id", id)
                                          .param("userId", userId)
                                          .update();

        if (affectedRows == 0) {
            throw new NotFoundException("Sesión no encontrada");
        }
    }

    /**
     * Agrega un ejercicio a una sesión de entrenamiento.
     * El ejercicio se inserta con notas nulas — se editan posteriormente.
     * Retorna el detalle completo de la sesión actualizada.
     *
     * @param id         id de la sesión
     * @param exerciseId id del ejercicio del catálogo a agregar
     * @param orderIndex posición del ejercicio dentro de la sesión
     * @param userId     id del usuario propietario
     * @return TrainingSessionDetailResponse con la sesión actualizada
     */
    public TrainingSessionDetailResponse saveExercise(Long id,
                                                      Long exerciseId,
                                                      Integer orderIndex,
                                                      Long userId) {
        this.jdbcClient.sql("INSERT INTO session_exercises (session_id, exercise_id, order_index, notes) " +
                            "VALUES (:id, :exerciseId, :orderIndex, NULL)")
                       .param("id", id)
                       .param("exerciseId", exerciseId)
                       .param("orderIndex", orderIndex)
                       .update();

        return this.findById(id, userId);
    }

    /**
     * Elimina un ejercicio de una sesión de entrenamiento.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los sets asociados al ejercicio eliminado.
     * Retorna el detalle completo de la sesión actualizada.
     *
     * @param id                 id de la sesión
     * @param sessionExerciseId  id del registro en session_exercises a eliminar
     * @param userId             id del usuario propietario
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el ejercicio no existe en la sesión
     */
    public TrainingSessionDetailResponse deleteExerciseById(Long id,
                                                            Long sessionExerciseId,
                                                            Long userId) {
        int affectedRows = this.jdbcClient.sql("DELETE FROM session_exercises " +
                                               "WHERE session_id = :id " +
                                               "AND id = :sessionExerciseId")
                                          .param("id", id)
                                          .param("sessionExerciseId", sessionExerciseId)
                                          .update();

        if  (affectedRows == 0) {
            throw new NotFoundException("Ejercicio no encontrado para sesión especificada");
        }

        return this.findById(id,
                             userId);
    }

    /**
     * Actualiza los datos de un ejercicio dentro de una sesión.
     * Usa COALESCE para actualizar solo los campos enviados.
     * Si se cambia el exerciseId, los sets deben eliminarse previamente
     * desde el service antes de llamar este método.
     * Retorna el detalle completo de la sesión actualizada.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises a actualizar
     * @param exerciseId        nuevo ejercicio — puede ser null para mantener el actual
     * @param orderIndex        nueva posición — puede ser null para mantener la actual
     * @param notes             nuevas notas — puede ser null para mantener las actuales
     * @param userId            id del usuario propietario
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el ejercicio no existe en la sesión
     */
    public TrainingSessionDetailResponse updateExercise(Long id,
                                                        Long sessionExerciseId,
                                                        Long exerciseId,
                                                        Integer orderIndex,
                                                        String notes,
                                                        Long userId) {
        int affectedRows = this.jdbcClient.sql("UPDATE session_exercises " +
                                               "SET exercise_id = COALESCE(:exerciseId, exercise_id), " +
                                                   "order_index = COALESCE(:orderIndex, order_index), " +
                                                   "notes = COALESCE(:notes, notes) " +
                                               "WHERE id = :sessionExerciseId")
                .param("exerciseId", exerciseId)
                .param("orderIndex", orderIndex)
                .param("notes", notes)
                .param("sessionExerciseId", sessionExerciseId)
                .update();

        if (affectedRows == 0) {
            throw new NotFoundException("Ejercicio no encontrado para sesión especificada");
        }

        return this.findById(id,
                             userId);
    }

    /**
     * Agrega un set vacío a un ejercicio de una sesión.
     * El set se crea con peso 0 y reps 0 para ser editado
     * con los valores reales del entrenamiento.
     * Retorna el detalle completo de la sesión actualizada.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises
     * @param setNumber         número de serie dentro del ejercicio
     * @param userId            id del usuario propietario
     * @return TrainingSessionDetailResponse con la sesión actualizada
     */
    public TrainingSessionDetailResponse saveExerciseSet(Long id,
                                                         Long sessionExerciseId,
                                                         Integer setNumber,
                                                         Long userId) {
        this.jdbcClient.sql("INSERT INTO sets (session_exercise_id, set_number, weight, reps, notes) " +
                            "VALUES (:sessionExerciseId, :setNumber, 0, 0, NULL)")
                .param("sessionExerciseId", sessionExerciseId)
                .param("setNumber", setNumber)
                .update();

        return this.findById(id,
                             userId);
    }

    /**
     * Elimina un set de un ejercicio de una sesión.
     * Retorna el detalle completo de la sesión actualizada.
     *
     * @param id                id de la sesión
     * @param setId             id del set a eliminar
     * @param sessionExerciseId id del registro en session_exercises
     * @param userId            id del usuario propietario
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el set no existe para el ejercicio especificado
     */
    public TrainingSessionDetailResponse deleteExerciseSetById(Long id,
                                                               Long setId,
                                                               Long sessionExerciseId,
                                                               Long userId) {
        int affectedRows = this.jdbcClient.sql("DELETE FROM sets " +
                                               "WHERE id = :setId " +
                                               "AND session_exercise_id = :sessionExerciseId")
                                          .param("setId", setId)
                                          .param("sessionExerciseId", sessionExerciseId)
                                          .update();

        if (affectedRows == 0) {
            throw new NotFoundException("Set no encontrado para ejercicio especificado");
        }

        return this.findById(id,
                             userId);
    }

    /**
     * Actualiza los datos de un set de un ejercicio de una sesión.
     * Usa COALESCE para actualizar solo los campos enviados.
     * Retorna el detalle completo de la sesión actualizada.
     *
     * @param id                id de la sesión
     * @param setId             id del set a actualizar
     * @param sessionExerciseId id del registro en session_exercises
     * @param setNumber         nuevo número de serie — puede ser null
     * @param weight            nuevo peso en kg — puede ser null
     * @param reps              nuevas repeticiones — puede ser null
     * @param notes             nuevas notas — puede ser null
     * @param userId            id del usuario propietario
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el set no existe para el ejercicio especificado
     */
    public TrainingSessionDetailResponse updateExerciseSet(Long id,
                                                           Long setId,
                                                           Long sessionExerciseId,
                                                           Integer setNumber,
                                                           Double weight,
                                                           Integer reps,
                                                           String notes,
                                                           Long userId) {
        int affectedRows = this.jdbcClient.sql("UPDATE sets " +
                                               "SET set_number = COALESCE(:setNumber, set_number), " +
                                                   "weight = COALESCE(:weight, weight), " +
                                                   "reps = COALESCE(:reps, reps), " +
                                                   "notes = COALESCE(:notes, notes) " +
                                               "WHERE id = :setId " +
                                               "AND session_exercise_id = :sessionExerciseId")
                                          .param("setNumber", setNumber)
                                          .param("weight", weight)
                                          .param("reps", reps)
                                          .param("notes", notes)
                                          .param("setId", setId)
                                          .param("sessionExerciseId", sessionExerciseId)
                                          .update();

        if (affectedRows == 0) {
            throw new NotFoundException("Set no encontrado para ejercicio especificado");
        }

        return this.findById(id,
                             userId);
    }

    /**
     * Elimina todos los sets asociados a un ejercicio de una sesión.
     * Se utiliza cuando el ejercicio de una sesión es reemplazado por otro,
     * ya que los sets del ejercicio anterior no son relevantes para el nuevo.
     *
     * @param sessionExerciseId id del registro en session_exercises
     *                          cuyos sets serán eliminados
     */
    public void deleteSetsFromSessionExercise(Long sessionExerciseId) {
        this.jdbcClient.sql("DELETE FROM sets " +
                            "WHERE session_exercise_id = :sessionExerciseId")
                       .param("sessionExerciseId", sessionExerciseId)
                       .update();
    }

    /**
     * Record interno usado para mapear filas de ejercicios durante la copia
     * hacia una nueva sesión — se reutiliza tanto para routine_exercises
     * (plantilla de rutina) como para session_exercises (última sesión),
     * ya que ambas tablas comparten el mismo esquema de columnas relevantes.
     * Proyección parcial — solo los campos necesarios para la copia.
     *
     * @param id          id del registro origen (en routine_exercises o session_exercises)
     * @param exerciseId  id del ejercicio del catálogo asociado
     * @param orderIndex  orden del ejercicio dentro de la rutina o sesión
     * @param notes       notas opcionales del ejercicio
     */
    private record ExerciseCopyRow(Long id,
                                   Long exerciseId,
                                   Integer orderIndex,
                                   String notes) {}

    /**
     * Record interno usado para mapear filas de sets durante la copia
     * hacia una nueva sesión — se reutiliza tanto para routine_sets
     * (plantilla de rutina) como para sets (última sesión), ya que ambas
     * tablas comparten el mismo esquema de columnas relevantes.
     * Proyección parcial — solo los campos necesarios para la copia.
     *
     * @param setNumber número de serie dentro del ejercicio
     * @param weight    peso en kilogramos
     * @param reps      repeticiones
     * @param notes     notas opcionales del set
     */
    private record SetCopyRow(Integer setNumber,
                              Double weight,
                              Integer reps,
                              String notes) {}
}

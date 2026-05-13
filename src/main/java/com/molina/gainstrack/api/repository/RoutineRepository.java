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

/**
 * Repositorio de acceso a datos para la tabla routines.
 * Ejecuta SQL puro mediante JdbcClient, sin ORM.
 * Todas las operaciones están acotadas al usuario propietario
 * para garantizar aislamiento de datos entre usuarios.
 */
@Repository
public class RoutineRepository {

    private final JdbcClient jdbcClient;

    /**
     * @param jdbcClient cliente JDBC para ejecutar consultas SQL
     */
    public RoutineRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Retorna el resumen de todas las rutinas del usuario.
     * No incluye ejercicios ni sets — usar findById para el detalle completo.
     *
     * @param userId id del usuario autenticado
     * @return lista de rutinas con sus datos de cabecera
     */
    public List<RoutineSummaryResponse> findAll(Long userId) {
        return this.jdbcClient.sql("SELECT id, name, created_at, notes, is_free " +
                                   "FROM routines " +
                                   "WHERE user_id = :userId")
                              .param("userId", userId)
                              .query(RoutineSummaryResponse.class)
                              .list();
    }

    /**
     * Retorna el detalle completo de una rutina.
     * Ejecuta tres queries anidadas para construir la respuesta:
     * 1. Datos de la rutina
     * 2. Ejercicios de la rutina con sus datos del catálogo
     * 3. Sets de referencia de cada ejercicio
     * El userId garantiza que el usuario solo pueda ver sus propias rutinas.
     *
     * @param id     id de la rutina a consultar
     * @param userId id del usuario propietario — previene acceso a rutinas ajenas
     * @return RoutineDetailResponse con ejercicios y sets anidados
     */
    public RoutineDetailResponse findById(Long id, Long userId) {
        return jdbcClient.sql("SELECT id AS routine_id, " +
                                     "name AS routine_name, " +
                                     "created_at AS routine_created_at, " +
                                     "notes AS routine_notes, " +
                                     "is_free AS routine_is_free " +
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
                                     rs.getBoolean("routine_is_free"),
                                     routineExercises
                             );
                         })
                         .single();
    }

    /**
     * Crea una nueva rutina para el usuario autenticado.
     * La rutina se crea con is_free = FALSE por defecto.
     * Usa KeyHolder para obtener el id generado y retornar el registro creado.
     *
     * @param userId id del usuario autenticado
     * @param name   nombre de la rutina
     * @param notes  notas opcionales de la rutina
     * @return RoutineSummaryResponse con los datos de la rutina creada
     */
    public RoutineSummaryResponse save(Long userId, String name, String notes) {
        // Se crea rutina con datos básicos
        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql("INSERT INTO routines (user_id, name, notes) " +
                            "VALUES (:userId, :name, :notes)")
                       .param("userId", userId)
                       .param("name", name)
                       .param("notes", notes)
                       .update(keyHolder);
        Long routineId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        return this.jdbcClient.sql("SELECT id, name, created_at, notes, is_free " +
                                   "FROM routines " +
                                   "WHERE id = :routineId AND user_id = :userId")
                              .param("routineId", routineId)
                              .param("userId", userId)
                              .query(RoutineSummaryResponse.class)
                              .single();
    }

    /**
     * Actualiza nombre y/o notas de una rutina.
     * Usa COALESCE para actualizar solo los campos enviados —
     * si un campo llega null, conserva el valor actual en la base de datos.
     * El userId garantiza que el usuario solo pueda editar sus propias rutinas.
     *
     * @param id     id de la rutina a actualizar
     * @param userId id del usuario propietario — previene edición de rutinas ajenas
     * @param name   nuevo nombre — puede ser null para mantener el actual
     * @param notes  nuevas notas — puede ser null para mantener las actuales
     */
    public void update(Long id, Long userId, String name, String notes) {
        this.jdbcClient.sql("UPDATE routines " +
                            "SET name = COALESCE(:name, name), notes = COALESCE(:notes, notes) " +
                            "WHERE id = :id " +
                            "AND user_id = :userId")
                       .param("name", name)
                       .param("notes", notes)
                       .param("id", id)
                       .param("userId", userId)
                       .update();
    }

    /**
     * Elimina una rutina por su id.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los routine_exercises, routine_sets y training_sessions asociados.
     * El userId garantiza que el usuario solo pueda eliminar sus propias rutinas.
     * La validación de is_free se realiza en el service antes de llamar este método.
     *
     * @param id     id de la rutina a eliminar
     * @param userId id del usuario propietario — previene eliminación de rutinas ajenas
     */
    public void deleteById(Long id, Long userId) {
        this.jdbcClient.sql("DELETE FROM routines " +
                            "WHERE id = :id AND user_id = :userId")
                       .param("id", id)
                       .param("userId", userId)
                       .update();
    }

    /**
     * Agrega un ejercicio a una rutina existente.
     * El ejercicio se inserta con notas nulas — se editan posteriormente.
     * Retorna el detalle completo de la rutina actualizada.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio del catálogo a agregar
     * @param orderIndex posición del ejercicio dentro de la rutina
     * @param userId     id del usuario propietario
     * @return RoutineDetailResponse con la rutina actualizada
     */
    public RoutineDetailResponse saveExercise(Long id,
                                              Long exerciseId,
                                              Integer orderIndex,
                                              Long userId) {
        this.jdbcClient.sql("INSERT INTO routine_exercises (routine_id, exercise_id, order_index, notes) " +
                            "VALUES (:id, :exerciseId, :orderIndex, NULL)")
                       .param("id", id)
                       .param("exerciseId", exerciseId)
                       .param("orderIndex", orderIndex)
                       .update();

        return this.findById(id, userId);
    }

    /**
     * Elimina un ejercicio de una rutina.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los routine_sets asociados al ejercicio eliminado.
     * Retorna el detalle completo de la rutina actualizada.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio a eliminar
     * @param userId     id del usuario propietario
     * @return RoutineDetailResponse con la rutina actualizada
     */
    public RoutineDetailResponse deleteExerciseById(Long id,
                                                    Long exerciseId,
                                                    Long userId) {
        this.jdbcClient.sql("DELETE FROM routine_exercises " +
                            "WHERE routine_id = :id " +
                            "AND exercise_id = :exerciseId")
                       .param("id", id)
                       .param("exerciseId", exerciseId)
                       .update();

        return this.findById(id,
                             userId);
    }

    /**
     * Agrega un set vacío a un ejercicio de una rutina.
     * El set se crea con peso 0 y reps 0 — se editan posteriormente
     * con los valores reales del entrenamiento.
     * Retorna el detalle completo de la rutina actualizada.
     *
     * @param id                 id de la rutina
     * @param routineExerciseId  id del registro en routine_exercises
     * @param setNumber          número de serie dentro del ejercicio
     * @param userId             id del usuario propietario
     * @return RoutineDetailResponse con la rutina actualizada
     */
    public RoutineDetailResponse saveExerciseSet(Long id,
                                                 Long routineExerciseId,
                                                 Integer setNumber,
                                                 Long userId) {
        this.jdbcClient.sql("INSERT INTO routine_sets (routine_exercise_id, set_number, weight, reps, notes) " +
                            "VALUES (:routineExerciseId, :setNumber, 0, 0, NULL)")
                       .param("routineExerciseId", routineExerciseId)
                       .param("setNumber", setNumber)
                       .update();

        return this.findById(id,
                             userId);
    }

    /**
     * Elimina un set de un ejercicio de una rutina.
     * Retorna el detalle completo de la rutina actualizada.
     *
     * @param id                id de la rutina
     * @param setId             id del set a eliminar
     * @param routineExerciseId id del registro en routine_exercises
     * @param userId            id del usuario propietario
     * @return RoutineDetailResponse con la rutina actualizada
     */
    public RoutineDetailResponse deleteExerciseSetById(Long id,
                                                       Long setId,
                                                       Long routineExerciseId,
                                                       Long userId) {
        this.jdbcClient.sql("DELETE FROM routine_sets " +
                            "WHERE id = :setId " +
                            "AND routine_exercise_id = :routineExerciseId")
                       .param("setId", setId)
                       .param("routineExerciseId", routineExerciseId)
                       .update();

        return this.findById(id,
                             userId);
    }

    /**
     * Actualiza los datos de un ejercicio dentro de una rutina.
     * Usa COALESCE para actualizar solo los campos enviados.
     * Permite reemplazar el ejercicio por otro o cambiar su orden y notas.
     * Retorna el detalle completo de la rutina actualizada.
     *
     * @param id                id de la rutina
     * @param routineExerciseId id del registro en routine_exercises
     * @param exerciseId        nuevo ejercicio — puede ser null para mantener el actual
     * @param orderIndex        nueva posición — puede ser null para mantener la actual
     * @param notes             nuevas notas — puede ser null para mantener las actuales
     * @param userId            id del usuario propietario
     * @return RoutineDetailResponse con la rutina actualizada
     */
    public RoutineDetailResponse updateExercise(Long id,
                                                Long routineExerciseId,
                                                Long exerciseId,
                                                Integer orderIndex,
                                                String notes,
                                                Long userId) {
        this.jdbcClient.sql("UPDATE routine_exercises " +
                            "SET exercise_id = COALESCE(:exerciseId, exercise_id), " +
                                "order_index = COALESCE(:orderIndex, order_index), " +
                                "notes = COALESCE(:notes, notes) " +
                            "WHERE id = :routineExerciseId")
                       .param("exerciseId", exerciseId)
                       .param("orderIndex", orderIndex)
                       .param("notes", notes)
                       .param("routineExerciseId", routineExerciseId)
                       .update();

        return this.findById(id,
                             userId);
    }

    /**
     * Actualiza los datos de un set de un ejercicio de una rutina.
     * Usa COALESCE para actualizar solo los campos enviados.
     * Retorna el detalle completo de la rutina actualizada.
     *
     * @param id                id de la rutina
     * @param setId             id del set a actualizar
     * @param routineExerciseId id del registro en routine_exercises
     * @param setNumber         nuevo número de serie — puede ser null
     * @param weight            nuevo peso en kg — puede ser null
     * @param reps              nuevas repeticiones — puede ser null
     * @param notes             nuevas notas — puede ser null
     * @param userId            id del usuario propietario
     * @return RoutineDetailResponse con la rutina actualizada
     */
    public RoutineDetailResponse updateExerciseSet(Long id,
                                                   Long setId,
                                                   Long routineExerciseId,
                                                   Integer setNumber,
                                                   Double weight,
                                                   Integer reps,
                                                   String notes,
                                                   Long userId) {
        this.jdbcClient.sql("UPDATE routine_sets " +
                            "SET set_number = COALESCE(:setNumber, set_number), " +
                                "weight = COALESCE(:weight, weight), " +
                                "reps = COALESCE(:reps, reps), " +
                                "notes = COALESCE(:notes, notes) " +
                            "WHERE id = :setId " +
                            "AND routine_exercise_id = :routineExerciseId")
                       .param("setNumber", setNumber)
                       .param("weight", weight)
                       .param("reps", reps)
                       .param("notes", notes)
                       .param("setId", setId)
                       .param("routineExerciseId", routineExerciseId)
                       .update();

        return this.findById(id,
                             userId);
    }

    public void saveFree(Long userId) {
        this.jdbcClient.sql("INSERT INTO routines (user_id, name, notes, is_free) " +
                            "VALUES (:userId, 'Rutina Libre', NULL, TRUE)")
                       .param("userId", userId)
                       .update();
    }
}

package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.routine.*;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.ExerciseRepository;
import com.molina.gainstrack.api.repository.RoutineRepository;
import com.molina.gainstrack.api.utils.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que maneja la lógica de negocio para la gestión de rutinas.
 * Obtiene el usuario autenticado desde AuthUtils para garantizar
 * que cada operación esté acotada al usuario en sesión.
 */
@Service
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final AuthUtils authUtils;

    /**
     * @param routineRepository repositorio de acceso a datos de rutinas
     * @param authUtils         utilidad para obtener el usuario autenticado
     */
    public RoutineService(RoutineRepository routineRepository,
                          ExerciseRepository exerciseRepository,
                          AuthUtils authUtils) {
        this.routineRepository = routineRepository;
        this.exerciseRepository = exerciseRepository;
        this.authUtils = authUtils;
    }

    /**
     * Retorna el resumen de todas las rutinas del usuario autenticado.
     * No incluye ejercicios ni sets.
     *
     * @return lista de rutinas del usuario en sesión
     */
    public List<RoutineSummaryResponse> findAll() {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.findAll(user.getId());
    }

    /**
     * Retorna el detalle completo de una rutina del usuario autenticado.
     * Incluye ejercicios con sus sets de referencia.
     *
     * @param id id de la rutina a consultar
     * @return RoutineDetailResponse con ejercicios y sets anidados
     */
    public RoutineDetailResponse findById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.findById(id,
                                               user.getId());
    }

    /**
     * Crea una nueva rutina para el usuario autenticado.
     *
     * @param request datos de la rutina — nombre obligatorio, notas opcionales
     * @return RoutineSummaryResponse con los datos de la rutina creada
     */
    public RoutineSummaryResponse save(RoutineRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.save(user.getId(),
                                           request.name(),
                                           request.notes());
    }

    /**
     * Actualiza nombre y/o notas de una rutina del usuario autenticado.
     * Solo actualiza los campos enviados — campos null conservan su valor actual.
     *
     * @param id      id de la rutina a actualizar
     * @param request campos a actualizar — name y/o notes, ambos opcionales
     */
    public void update(Long id, RoutineRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        this.routineRepository.update(id,
                                      user.getId(),
                                      request.name(),
                                      request.notes());
    }

    /**
     * Elimina una rutina del usuario autenticado.
     * La rutina especial is_free no se puede eliminar.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los ejercicios, sets y sesiones asociadas a esta rutina.
     *
     * @param id id de la rutina a eliminar
     * @throws RuntimeException si se intenta eliminar la rutina libre
     */
    public void deleteById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id, user.getId());

        if (routine.isFree())
            throw new RuntimeException("La rutina libre no se puede eliminar");

        this.routineRepository.deleteById(id, user.getId());
    }

    /**
     * Agrega un ejercicio al final de una rutina del usuario autenticado.
     * Valida que el ejercicio exista en el catálogo antes de insertarlo.
     *
     * @param id      id de la rutina
     * @param request datos del ejercicio a agregar — exerciseId y orderIndex
     * @return RoutineDetailResponse con la rutina actualizada
     * @throws RuntimeException si el ejercicio no existe en el catálogo
     */
    public RoutineDetailResponse saveExercise(Long id,
                                              RoutineExerciseRequest request) {
        User user = this.authUtils.getAuthenticatedUser();

        if (!exerciseRepository.existsById(request.exerciseId()))
            throw new RuntimeException("Ejercicio no encontrado");

        return this.routineRepository.saveExercise(id,
                                                   request.exerciseId(),
                                                   request.orderIndex(),
                                                   user.getId());
    }

    /**
     * Elimina un ejercicio de una rutina del usuario autenticado.
     * Valida que el ejercicio pertenezca a la rutina antes de eliminar.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio a eliminar
     * @return RoutineDetailResponse con la rutina actualizada
     * @throws RuntimeException si el ejercicio no pertenece a la rutina
     */
    public RoutineDetailResponse deleteExerciseById(Long id,
                                                    Long exerciseId) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id,
                                                                        user.getId());
        boolean isExerciseFromRoutine = routine.exercises()
                                               .stream()
                                               .anyMatch(routineExercise -> routineExercise.exercise()
                                                                                                                 .id()
                                                                                                                 .equals(exerciseId));

        if (!isExerciseFromRoutine)
            throw new RuntimeException("Ejercicio no pertenece a rutina especificada");

        return this.routineRepository.deleteExerciseById(id,
                                                         exerciseId,
                                                         user.getId());
    }

    /**
     * Agrega un set vacío a un ejercicio de una rutina del usuario autenticado.
     * Valida que el ejercicio pertenezca a la rutina antes de insertar.
     * El set se crea con peso 0 y reps 0 para ser editado posteriormente.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio al que agregar el set
     * @param request    datos del set — solo setNumber
     * @return RoutineDetailResponse con la rutina actualizada
     * @throws RuntimeException si el ejercicio no pertenece a la rutina
     */
    public RoutineDetailResponse saveExerciseSet(Long id,
                                                 Long exerciseId,
                                                 RoutineSetRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id,
                                                                        user.getId());
        Long routineExerciseId = routine.exercises()
                                        .stream()
                                        .filter(routineExercise -> routineExercise.exercise()
                                                                                                        .id()
                                                                                                        .equals(exerciseId))
                                        .map(RoutineExerciseResponse::id)
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Ejercicio no pertenece a rutina especificada"));

        return this.routineRepository.saveExerciseSet(id,
                                                      routineExerciseId,
                                                      request.setNumber(),
                                                      user.getId());
    }

    /**
     * Elimina un set de un ejercicio de una rutina del usuario autenticado.
     * Valida que el ejercicio pertenezca a la rutina antes de eliminar.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio al que pertenece el set
     * @param setId      id del set a eliminar
     * @return RoutineDetailResponse con la rutina actualizada
     * @throws RuntimeException si el ejercicio no pertenece a la rutina
     */
    public RoutineDetailResponse deleteExerciseSetById(Long id,
                                                       Long exerciseId,
                                                       Long setId) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id,
                                                                        user.getId());
        Long routineExerciseId = routine.exercises()
                                        .stream()
                                        .filter(routineExercise -> routineExercise.exercise()
                                                                                                        .id()
                                                                                                        .equals(exerciseId))
                                        .map(RoutineExerciseResponse::id)
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Ejercicio no pertenece a rutina especificada"));

        return this.routineRepository.deleteExerciseSetById(id,
                                                            setId,
                                                            routineExerciseId,
                                                            user.getId());
    }

    /**
     * Actualiza los datos de un ejercicio dentro de una rutina del usuario autenticado.
     * Solo actualiza los campos enviados — campos null conservan su valor actual.
     * Permite reemplazar el ejercicio, cambiar su orden o actualizar sus notas.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio a actualizar
     * @param request    campos a actualizar — exerciseId, orderIndex y/o notes
     * @return RoutineDetailResponse con la rutina actualizada
     * @throws RuntimeException si el ejercicio no pertenece a la rutina
     */
    public RoutineDetailResponse updateExercise(Long id,
                                                Long exerciseId,
                                                RoutineExerciseUpdateRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id,
                                                                        user.getId());

        Long routineExerciseId = routine.exercises()
                                        .stream()
                                        .filter(routineExercise -> routineExercise.exercise()
                                                                                                        .id()
                                                                                                        .equals(exerciseId))
                                        .map(RoutineExerciseResponse::id)
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Ejercicio no pertenece a rutina especificada"));
        return this.routineRepository.updateExercise(id,
                                                     routineExerciseId,
                                                     exerciseId,
                                                     request.orderIndex(),
                                                     request.notes(),
                                                     user.getId());
    }

    /**
     * Actualiza los datos de un set de un ejercicio de una rutina del usuario autenticado.
     * Solo actualiza los campos enviados — campos null conservan su valor actual.
     *
     * @param id         id de la rutina
     * @param exerciseId id del ejercicio al que pertenece el set
     * @param setId      id del set a actualizar
     * @param request    campos a actualizar — setNumber, weight, reps y/o notes
     * @return RoutineDetailResponse con la rutina actualizada
     * @throws RuntimeException si el ejercicio no pertenece a la rutina
     */
    public RoutineDetailResponse updateExerciseSet(Long id,
                                                   Long exerciseId,
                                                   Long setId,
                                                   RoutineSetRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id,
                                                                        user.getId());

        Long routineExerciseId = routine.exercises()
                                        .stream()
                                        .filter(routineExercise -> routineExercise.exercise()
                                                                                                        .id()
                                                                                                        .equals(exerciseId))
                                        .map(RoutineExerciseResponse::id)
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Ejercicio no pertenece a rutina especificada"));

        return this.routineRepository.updateExerciseSet(id,
                                                        setId,
                                                        routineExerciseId,
                                                        request.setNumber(),
                                                        request.weight(),
                                                        request.reps(),
                                                        request.notes(),
                                                        user.getId());
    }

    public void saveFree(Long userId) {
        this.routineRepository.saveFree(userId);
    }
}

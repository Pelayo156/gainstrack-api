package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.session.*;
import com.molina.gainstrack.api.exception.NotFoundException;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.ExerciseRepository;
import com.molina.gainstrack.api.repository.TrainingSessionRepository;
import com.molina.gainstrack.api.utils.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que maneja la lógica de negocio para las sesiones de entrenamiento.
 * Obtiene el usuario autenticado desde AuthUtils para garantizar
 * que cada operación esté acotada al usuario en sesión.
 */
@Service
public class TrainingSessionService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainingSessionService.class);

    private final TrainingSessionRepository trainingSessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final AuthUtils authUtils;

    /**
     * @param trainingSessionRepository repositorio de acceso a datos de sesiones
     * @param authUtils                 utilidad para obtener el usuario autenticado
     */
    public TrainingSessionService(TrainingSessionRepository trainingSessionRepository,
                                  ExerciseRepository exerciseRepository,
                                  AuthUtils authUtils) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.authUtils = authUtils;
    }

    /**
     * Retorna el resumen de todas las sesiones del usuario autenticado.
     * Cada sesión incluye su gimnasio asociado pero no el detalle
     * de ejercicios ni sets.
     *
     * @return lista de sesiones de entrenamiento del usuario en sesión
     */
    public List<TrainingSessionSummaryResponse> findAll() {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.findAll(user.getId());
    }

    /**
     * Crea una nueva sesión de entrenamiento para el usuario autenticado.
     * El punto de partida de ejercicios y sets depende del historial del usuario
     * para la misma rutina y el mismo gimnasio (gymId null incluido, como "sin
     * gimnasio"/sesión libre): si existe una sesión previa se copian sus ejercicios
     * y sets reales; si es la primera vez, se copian desde la plantilla de la rutina.
     * La fecha se asigna automáticamente como la fecha actual del servidor.
     *
     * @param request datos de la sesión — routineId obligatorio, gymId y notes opcionales
     * @return TrainingSessionDetailResponse con el detalle completo de la sesión creada
     */
    @Transactional
    public TrainingSessionDetailResponse save(TrainingSessionRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        TrainingSessionDetailResponse session = this.trainingSessionRepository.save(user.getId(),
                                                                                    request.gymId(),
                                                                                    request.routineId(),
                                                                                    request.notes());
        LOG.info("Nueva sesión creada — sessionId: {}, userId: {}, routineId: {}",
                session.id(), user.getId(), request.routineId());
        return session;
    }

    /**
     * Retorna el detalle completo de una sesión del usuario autenticado.
     * Incluye ejercicios realizados con sus sets y pesos registrados.
     *
     * @param id id de la sesión a consultar
     * @return TrainingSessionDetailResponse con ejercicios y sets anidados
     */
    public TrainingSessionDetailResponse findById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.findById(id, user.getId());
    }

    /**
     * Retorna el resumen de la última sesión del usuario autenticado para una
     * rutina y gimnasio específicos. Se usa en el flujo de inicio de entrenamiento:
     * el usuario elige el gimnasio donde va a entrenar y el frontend consulta la
     * última sesión de esa rutina en ese gimnasio antes de crear la nueva sesión.
     * gymId puede ser null para consultar sesiones sin gimnasio asociado
     * (sesión libre de gimnasio) — se trata como un gimnasio más.
     *
     * @param routineId id de la rutina consultada
     * @param gymId     id del gimnasio consultado — null representa sin gimnasio
     * @return TrainingSessionSummaryResponse con el resumen de la última sesión
     * @throws NotFoundException si no existe una sesión previa para esa combinación
     */
    public TrainingSessionSummaryResponse findLastByRoutineAndGym(Long routineId, Long gymId) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.findLastByRoutineAndGym(routineId, gymId, user.getId());
    }

    /**
     * Elimina una sesión de entrenamiento del usuario autenticado.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los ejercicios y sets asociados a esta sesión.
     *
     * @param id id de la sesión a eliminar
     */
    public void deleteById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        trainingSessionRepository.deleteById(id, user.getId());
        LOG.warn("Sesión eliminada — sessionId: {}, userId: {}", id, user.getId());
    }

    /**
     * Actualiza las notas de una sesión del usuario autenticado.
     * Solo modifica el campo notes — la fecha y rutina son inmutables.
     *
     * @param id      id de la sesión a actualizar
     * @param request datos a actualizar — solo notes
     * @throws NotFoundException si la sesión no existe
     */
    public void update(Long id, TrainingSessionNotesRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        this.trainingSessionRepository.update(id, user.getId(), request.notes());
    }

    /**
     * Agrega un ejercicio a una sesión del usuario autenticado.
     * Valida que el ejercicio exista en el catálogo antes de insertarlo.
     *
     * @param id      id de la sesión
     * @param request datos del ejercicio a agregar — exerciseId y orderIndex
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el ejercicio no existe en el catálogo
     */
    @Transactional
    public TrainingSessionDetailResponse saveExercise(Long id,
                                                      TrainingSessionExerciseRequest request) {
        User user = this.authUtils.getAuthenticatedUser();

        if (!exerciseRepository.existsById(request.exerciseId()))
            throw new NotFoundException("Ejercicio no encontrado");

        TrainingSessionDetailResponse session = this.trainingSessionRepository.saveExercise(id,
                                                                                            request.exerciseId(),
                                                                                            request.orderIndex(),
                                                                                            user.getId());
        LOG.info("Ejercicio agregado a sesión — sessionId: {}, exerciseId: {}",
                 id, request.exerciseId());
        return session;
    }

    /**
     * Elimina un ejercicio de una sesión del usuario autenticado.
     * Valida que el ejercicio pertenezca a la sesión antes de eliminar.
     * Por el CASCADE del modelo relacional, se eliminan también sus sets.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises a eliminar
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el sessionExerciseId no pertenece a la sesión
     */
    @Transactional
    public TrainingSessionDetailResponse deleteExerciseById(Long id,
                                                            Long sessionExerciseId) {
        User user = this.authUtils.getAuthenticatedUser();
        TrainingSessionDetailResponse session = this.trainingSessionRepository.findById(id,
                                                                                        user.getId());

        boolean isExerciseFromSession = session.exercises()
                                               .stream()
                                               .anyMatch(se -> se.id().equals(sessionExerciseId));

        if (!isExerciseFromSession)
            throw new NotFoundException("Ejercicio no encontrado para sesión especificada");

        TrainingSessionDetailResponse updated = this.trainingSessionRepository.deleteExerciseById(id,
                                                                                                  sessionExerciseId,
                                                                                                  user.getId());
        LOG.warn("Ejercicio eliminado de sesión — sessionId: {}, sessionExerciseId: {}",
                 id, sessionExerciseId);
        return updated;
    }

    /**
     * Actualiza los datos de un ejercicio dentro de una sesión del usuario autenticado.
     * Solo actualiza los campos enviados — campos null conservan su valor actual.
     * Si se cambia el exerciseId, elimina automáticamente los sets existentes
     * ya que pertenecían al ejercicio anterior.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises a actualizar
     * @param request           campos a actualizar — exerciseId, orderIndex y/o notes
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el sessionExerciseId no pertenece a la sesión
     */
    @Transactional
    public TrainingSessionDetailResponse updateExercise(Long id,
                                                        Long sessionExerciseId,
                                                        TrainingSessionExerciseRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        TrainingSessionDetailResponse session = this.trainingSessionRepository.findById(id,
                                                                                        user.getId());

        Long currentExerciseId = session.exercises()
                                        .stream()
                                        .filter(se -> se.id().equals(sessionExerciseId))
                                        .map(se -> se.exercise().id())
                                        .findFirst()
                                        .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado para sesión especificada"));

        if (request.exerciseId() != null && !currentExerciseId.equals(request.exerciseId())) {
            this.trainingSessionRepository.deleteSetsFromSessionExercise(sessionExerciseId);
            LOG.warn("Sets eliminados por cambio de ejercicio — sessionExerciseId: {}",
                     sessionExerciseId);
        }

        return this.trainingSessionRepository.updateExercise(id,
                                                             sessionExerciseId,
                                                             request.exerciseId(),
                                                             request.orderIndex(),
                                                             request.notes(),
                                                             user.getId());
    }

    /**
     * Agrega un set vacío a un ejercicio de una sesión del usuario autenticado.
     * Valida que el sessionExerciseId pertenezca a la sesión antes de insertar.
     * El set se crea con peso 0 y reps 0 para ser editado con los valores reales.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises al que agregar el set
     * @param request           datos del set — solo setNumber obligatorio
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el sessionExerciseId no pertenece a la sesión
     */
    @Transactional
    public TrainingSessionDetailResponse saveExerciseSet(Long id,
                                                         Long sessionExerciseId,
                                                         TrainingSessionSetRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        TrainingSessionDetailResponse session = this.trainingSessionRepository.findById(id,
                                                                                        user.getId());

        boolean isExerciseFromSession = session.exercises()
                                               .stream()
                                               .anyMatch(se -> se.id().equals(sessionExerciseId));

        if (!isExerciseFromSession)
            throw new NotFoundException("Ejercicio no encontrado para sesión especificada");

        TrainingSessionDetailResponse updated = this.trainingSessionRepository.saveExerciseSet(id,
                                                                                               sessionExerciseId,
                                                                                               request.setNumber(),
                                                                                               user.getId());
        LOG.info("Set agregado — sessionId: {}, sessionExerciseId: {}, setNumber: {}",
                 id,
                 sessionExerciseId,
                 request.setNumber());
        return updated;
    }

    /**
     * Elimina un set de un ejercicio de una sesión del usuario autenticado.
     * Valida que el sessionExerciseId pertenezca a la sesión antes de eliminar.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises al que pertenece el set
     * @param setId             id del set a eliminar
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el sessionExerciseId no pertenece a la sesión
     */
    @Transactional
    public TrainingSessionDetailResponse deleteExerciseSetById(Long id,
                                                               Long sessionExerciseId,
                                                               Long setId) {
        User user = this.authUtils.getAuthenticatedUser();
        TrainingSessionDetailResponse session = this.trainingSessionRepository.findById(id,
                                                                                        user.getId());

        boolean isExerciseFromSession = session.exercises()
                                               .stream()
                                               .anyMatch(se -> se.id().equals(sessionExerciseId));

        if (!isExerciseFromSession)
            throw new NotFoundException("Ejercicio no encontrado para sesión especificada");

        TrainingSessionDetailResponse updated = this.trainingSessionRepository.deleteExerciseSetById(id,
                                                                                                     setId,
                                                                                                     sessionExerciseId,
                                                                                                     user.getId());
        LOG.warn("Set eliminado — sessionId: {}, sessionExerciseId: {}, setId: {}",
                 id,
                 sessionExerciseId,
                 setId);
        return updated;
    }

    /**
     * Actualiza los datos de un set de un ejercicio de una sesión del usuario autenticado.
     * Solo actualiza los campos enviados — campos null conservan su valor actual.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises al que pertenece el set
     * @param setId             id del set a actualizar
     * @param request           campos a actualizar — setNumber, weight, reps y/o notes
     * @return TrainingSessionDetailResponse con la sesión actualizada
     * @throws NotFoundException si el sessionExerciseId no pertenece a la sesión
     */
    public TrainingSessionDetailResponse updateExerciseSet(Long id,
                                                           Long sessionExerciseId,
                                                           Long setId,
                                                           TrainingSessionSetRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        TrainingSessionDetailResponse session = this.trainingSessionRepository.findById(id,
                                                                                        user.getId());

        session.exercises()
                .stream()
                .filter(se -> se.id().equals(sessionExerciseId))
                .map(se -> se.exercise().id())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Ejercicio no encontrado para sesión especificada"));

        return this.trainingSessionRepository.updateExerciseSet(id,
                                                                setId,
                                                                sessionExerciseId,
                                                                request.setNumber(),
                                                                request.weight(),
                                                                request.reps(),
                                                                request.notes(),
                                                                user.getId());
    }
}
package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.session.*;
import com.molina.gainstrack.api.service.TrainingSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller que expone los endpoints REST para la gestión
 * de sesiones de entrenamiento.
 * Todas las operaciones requieren autenticación JWT.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    /**
     * @param trainingSessionService servicio con la lógica de sesiones de entrenamiento
     */
    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    /**
     * Retorna el resumen de todas las sesiones del usuario autenticado.
     * No incluye ejercicios ni sets — usar GET /{id} para el detalle completo.
     *
     * @return 200 OK con la lista de sesiones del usuario
     */
   @GetMapping
   public ResponseEntity<List<TrainingSessionSummaryResponse>> findAll() {
        return ResponseEntity.ok(this.trainingSessionService.findAll());
   }

    /**
     * Crea una nueva sesión ejecutando una rutina existente.
     * Copia automáticamente ejercicios y sets de la rutina como punto de partida.
     *
     * @param request body con routineId obligatorio, gymId y notes opcionales
     * @return 201 Created con el detalle completo de la sesión creada
     */
   @PostMapping
    public ResponseEntity<TrainingSessionDetailResponse> save(@Valid @RequestBody TrainingSessionRequest request) {
       return ResponseEntity.status(201).body(this.trainingSessionService.save(request));
   }

    /**
     * Retorna el detalle completo de una sesión del usuario autenticado.
     * Incluye ejercicios realizados con sus sets y pesos registrados.
     *
     * @param id id de la sesión a consultar
     * @return 200 OK con ejercicios y sets anidados
     */
   @GetMapping("/{id}")
   public ResponseEntity<TrainingSessionDetailResponse> findById(@PathVariable("id") Long id) {
       return ResponseEntity.ok(this.trainingSessionService.findById(id));
   }

    /**
     * Elimina una sesión de entrenamiento del usuario autenticado.
     * Advertencia: elimina también todos los ejercicios y sets asociados.
     *
     * @param id id de la sesión a eliminar
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        trainingSessionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Actualiza las notas de una sesión del usuario autenticado.
     *
     * @param id      id de la sesión a actualizar
     * @param request body con notes — opcional
     * @return 204 No Content
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @RequestBody TrainingSessionNotesRequest request) {
        this.trainingSessionService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Agrega un ejercicio a una sesión del usuario autenticado.
     *
     * @param id      id de la sesión
     * @param request body con exerciseId y orderIndex
     * @return 201 Created con la sesión actualizada
     */
    @PostMapping("/{id}/exercises")
    public ResponseEntity<TrainingSessionDetailResponse> saveExercise(@PathVariable("id") Long id,
                                                                      @Valid @RequestBody TrainingSessionExerciseRequest request) {
        return ResponseEntity.status(201).body(this.trainingSessionService.saveExercise(id,
                                                                                        request));
    }

    /**
     * Elimina un ejercicio de una sesión del usuario autenticado.
     * Advertencia: elimina también todos los sets asociados al ejercicio.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises a eliminar
     * @return 200 OK con la sesión actualizada
     */
    @DeleteMapping("/{id}/exercises/{sessionExerciseId}")
    public ResponseEntity<TrainingSessionDetailResponse> deleteExerciseById(@PathVariable("id") Long id,
                                                                            @PathVariable("sessionExerciseId") Long sessionExerciseId) {
        return ResponseEntity.ok(this.trainingSessionService.deleteExerciseById(id,
                                                                                sessionExerciseId));
    }

    /**
     * Actualiza los datos de un ejercicio dentro de una sesión del usuario autenticado.
     * Solo modifica los campos enviados en el body.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises a actualizar
     * @param request           body con exerciseId, orderIndex y/o notes — todos opcionales
     * @return 200 OK con la sesión actualizada
     */
    @PatchMapping("/{id}/exercises/{sessionExerciseId}")
    public ResponseEntity<TrainingSessionDetailResponse> updateExercise(@PathVariable("id") Long id,
                                                                        @PathVariable("sessionExerciseId") Long sessionExerciseId,
                                                                        @RequestBody TrainingSessionExerciseRequest request) {
        return ResponseEntity.ok(this.trainingSessionService.updateExercise(id,
                                                                            sessionExerciseId,
                                                                            request));
    }

    /**
     * Agrega un set vacío a un ejercicio de una sesión del usuario autenticado.
     * El set se crea con peso 0 y reps 0 para ser editado con los valores reales.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises al que agregar el set
     * @param request           body con setNumber
     * @return 201 Created con la sesión actualizada
     */
    @PostMapping("/{id}/exercises/{sessionExerciseId}/sets")
    public ResponseEntity<TrainingSessionDetailResponse> saveExerciseSet(@PathVariable("id") Long id,
                                                                         @PathVariable("sessionExerciseId") Long sessionExerciseId,
                                                                         @Valid @RequestBody TrainingSessionSetRequest request) {
        return ResponseEntity.status(201).body(this.trainingSessionService.saveExerciseSet(id,
                                                                                           sessionExerciseId,
                                                                                           request));
    }

    /**
     * Elimina un set de un ejercicio de una sesión del usuario autenticado.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises al que pertenece el set
     * @param setId             id del set a eliminar
     * @return 200 OK con la sesión actualizada
     */
    @DeleteMapping("/{id}/exercises/{sessionExerciseId}/sets/{setId}")
    public ResponseEntity<TrainingSessionDetailResponse> deleteExerciseSetById(@PathVariable("id") Long id,
                                                                       @PathVariable("sessionExerciseId") Long sessionExerciseId,
                                                                       @PathVariable("setId") Long setId) {
        return ResponseEntity.ok(this.trainingSessionService.deleteExerciseSetById(id,
                                                                                   sessionExerciseId,
                                                                                   setId));
    }

    /**
     * Actualiza los datos de un set de un ejercicio de una sesión del usuario autenticado.
     * Solo modifica los campos enviados en el body.
     *
     * @param id                id de la sesión
     * @param sessionExerciseId id del registro en session_exercises al que pertenece el set
     * @param setId             id del set a actualizar
     * @param request           body con setNumber, weight, reps y/o notes — todos opcionales
     * @return 200 OK con la sesión actualizada
     */
    @PatchMapping("/{id}/exercises/{sessionExerciseId}/sets/{setId}")
    public ResponseEntity<TrainingSessionDetailResponse> updateExerciseSet(@PathVariable("id") Long id,
                                                                           @PathVariable("sessionExerciseId") Long sessionExerciseId,
                                                                           @PathVariable("setId") Long setId,
                                                                           @RequestBody TrainingSessionSetRequest request) {
        return ResponseEntity.ok(this.trainingSessionService.updateExerciseSet(id,
                                                                               sessionExerciseId,
                                                                               setId,
                                                                               request));
    }
}

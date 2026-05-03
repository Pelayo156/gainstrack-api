package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.session.TrainingSessionDetailResponse;
import com.molina.gainstrack.api.dto.session.TrainingSessionRequest;
import com.molina.gainstrack.api.dto.session.TrainingSessionSummaryResponse;
import com.molina.gainstrack.api.service.TrainingSessionService;
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
    public ResponseEntity<TrainingSessionDetailResponse> save(@RequestBody TrainingSessionRequest request) {
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
       return ResponseEntity.ok().body(this.trainingSessionService.findById(id));
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
}

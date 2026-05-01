package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.TrainingSessionRequest;
import com.molina.gainstrack.api.dto.TrainingSessionSummaryResponse;
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

   @PostMapping
    public ResponseEntity<TrainingSessionSummaryResponse> save(@RequestBody TrainingSessionRequest request) {
       return ResponseEntity.status(201).body(this.trainingSessionService.save(request));
   }
}

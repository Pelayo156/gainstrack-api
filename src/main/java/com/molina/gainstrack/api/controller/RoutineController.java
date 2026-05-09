package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.routine.RoutineDetailResponse;
import com.molina.gainstrack.api.dto.routine.RoutineRequest;
import com.molina.gainstrack.api.dto.routine.RoutineSummaryResponse;
import com.molina.gainstrack.api.service.RoutineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller que expone los endpoints REST para la gestión de rutinas.
 * Todas las operaciones requieren autenticación JWT.
 */
@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;

    /**
     * @param routineService servicio con la lógica de negocio de rutinas
     */
    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    /**
     * Retorna el resumen de todas las rutinas del usuario autenticado.
     * No incluye ejercicios ni sets — usar GET /{id} para el detalle completo.
     *
     * @return 200 OK con la lista de rutinas del usuario
     */
    @GetMapping
    public ResponseEntity<List<RoutineSummaryResponse>> findAll() {
        return ResponseEntity.ok().body(this.routineService.findAll());
    }

    /**
     * Retorna el detalle completo de una rutina del usuario autenticado.
     * Incluye ejercicios con sus sets de referencia.
     *
     * @param id id de la rutina a consultar
     * @return 200 OK con ejercicios y sets anidados
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoutineDetailResponse> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(this.routineService.findById(id));
    }

    /**
     * Crea una nueva rutina para el usuario autenticado.
     *
     * @param request body con nombre obligatorio y notas opcionales
     * @return 201 Created con los datos de la rutina creada
     */
    @PostMapping
    public ResponseEntity<RoutineSummaryResponse> save(@RequestBody RoutineRequest request) {
        return ResponseEntity.status(201).body(this.routineService.save(request));
    }

    /**
     * Actualiza nombre y/o notas de una rutina del usuario autenticado.
     * Solo modifica los campos enviados en el body.
     *
     * @param id      id de la rutina a actualizar
     * @param request body con name y/o notes — ambos opcionales
     * @return 204 No Content
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @RequestBody RoutineRequest request) {
        this.routineService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina una rutina del usuario autenticado.
     * Advertencia: elimina también todos los ejercicios, sets y sesiones asociadas.
     * La rutina libre no se puede eliminar.
     *
     * @param id id de la rutina a eliminar
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        this.routineService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/exercises")
    public ResponseEntity<RoutineDetailResponse> saveExercise(@PathVariable("id") Long id,
                                                              Long exerciseId) {
        return ResponseEntity.status(201).body(this.routineService.saveExercise(id, exerciseId));
    }
}

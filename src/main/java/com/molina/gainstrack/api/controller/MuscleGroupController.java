package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.shared.MuscleGroupResponse;
import com.molina.gainstrack.api.service.MuscleGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller que expone los endpoints REST para los grupos musculares.
 * Los grupos musculares son datos de catálogo global — cualquier
 * usuario autenticado puede consultarlos.
 */
@RestController
@RequestMapping("/api/v1/muscle-groups")
public class MuscleGroupController {

    /**
     * @param muscleGroupService servicio con la lógica de grupos musculares
     */
    private final MuscleGroupService muscleGroupService;

    public MuscleGroupController(MuscleGroupService muscleGroupService) {
        this.muscleGroupService = muscleGroupService;
    }

    /**
     * Retorna todos los grupos musculares disponibles.
     * Se usa principalmente para poblar selectores en la UI
     * al momento de crear o editar un ejercicio.
     *
     * @return 200 OK con la lista completa de grupos musculares
     */
     @GetMapping
    public ResponseEntity<List<MuscleGroupResponse>> findAll() {
        return ResponseEntity.ok(muscleGroupService.findAll());
    }
}

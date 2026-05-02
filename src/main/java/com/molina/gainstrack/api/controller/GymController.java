package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.GymRequest;
import com.molina.gainstrack.api.dto.GymResponse;
import com.molina.gainstrack.api.service.GymService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller que expone los endpoints REST para la gestión de gimnasios.
 * Todas las operaciones requieren autenticación JWT.
 * El usuario autenticado se resuelve internamente en el Service
 * desde el contexto de seguridad — no se recibe como parámetro.
 */
@RestController
@RequestMapping("/api/v1/gyms")
public class GymController {

    /**
     * @param gymService servicio con la lógica de negocio de gimnasios
     */
    private final GymService gymService;

    public GymController(GymService gymService) { this.gymService = gymService; }

    /**
     * Crea un nuevo gimnasio para el usuario autenticado.
     *
     * @param request body con el nombre del gimnasio
     * @return 201 Created con los datos del gimnasio creado
     */
    @PostMapping
    public ResponseEntity<GymResponse> save(@RequestBody GymRequest request) {
        return ResponseEntity.status(201).body(gymService.save(request));
    }

    /**
     * Retorna todos los gimnasios del usuario autenticado.
     *
     * @return 200 OK con la lista de gimnasios del usuario
     */
    @GetMapping
    public ResponseEntity<List<GymResponse>> findAll() {
        return ResponseEntity.ok(gymService.findAll());
    }

    /**
     * Elimina un gimnasio por su id.
     * Advertencia: elimina también todas las sesiones asociadas.
     *
     * @param id id del gimnasio a eliminar
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        gymService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

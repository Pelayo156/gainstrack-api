package com.molina.gainstrack.api.dto.routine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar una rutina.
 * Ambos campos son opcionales en PATCH — solo se actualizan los enviados.
 *
 * @param name  nombre de la rutina
 * @param notes notas opcionales de la rutina
 */
public record RoutineRequest(
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.size.max}")
        String name,
        String notes
) {}

package com.molina.gainstrack.api.dto.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de entrada para agregar un ejercicio a una sesión.
 *
 * @param exerciseId id del ejercicio del catálogo a agregar
 * @param orderIndex posición del ejercicio dentro de la sesión
 */
public record TrainingSessionExerciseRequest(
        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Long exerciseId,

        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Integer orderIndex,
        String notes
) {}

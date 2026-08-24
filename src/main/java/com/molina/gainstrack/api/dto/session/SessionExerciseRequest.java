package com.molina.gainstrack.api.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO de entrada para un ejercicio dentro del body de creación de una
 * sesión de entrenamiento (POST /sessions). Se persiste tal cual —
 * a diferencia de TrainingSessionExerciseRequest, que agrega/actualiza
 * un ejercicio sobre una sesión ya existente.
 *
 * @param exerciseId id del ejercicio del catálogo
 * @param orderIndex posición del ejercicio dentro de la sesión
 * @param notes      notas del ejercicio — opcional
 * @param sets       sets realizados en este ejercicio — opcional, null se trata como lista vacía
 */
public record SessionExerciseRequest(
        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Long exerciseId,

        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Integer orderIndex,
        String notes,

        @Valid
        List<SessionSetRequest> sets
) {}

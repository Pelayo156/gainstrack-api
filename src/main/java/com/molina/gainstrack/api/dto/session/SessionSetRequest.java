package com.molina.gainstrack.api.dto.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de entrada para un set dentro del body de creación de una sesión
 * de entrenamiento (POST /sessions). Se persiste tal cual — a diferencia
 * de TrainingSessionSetRequest, que agrega/actualiza un set sobre un
 * ejercicio de una sesión ya existente.
 *
 * @param setNumber número de serie dentro del ejercicio
 * @param weight    peso en kilogramos — opcional, null lo deja sin definir
 * @param reps      repeticiones — opcional, null las deja sin definir
 * @param notes     notas opcionales del set — opcional, null las deja sin definir
 */
public record SessionSetRequest(
        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Integer setNumber,
        Double weight,
        Integer reps,
        String notes
) {}

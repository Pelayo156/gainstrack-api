package com.molina.gainstrack.api.dto.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de entrada para crear o actualizar un set de un ejercicio en una sesión.
 * En creación solo setNumber es obligatorio — peso y reps inician en 0.
 * En actualización todos los campos son opcionales — COALESCE preserva
 * los no enviados sin modificar su valor actual en la base de datos.
 *
 * @param setNumber número de serie dentro del ejercicio — obligatorio en creación
 * @param weight    peso en kilogramos — opcional, puede ser null
 * @param reps      repeticiones — opcional, puede ser null
 * @param notes     notas opcionales del set — puede ser null
 */
public record TrainingSessionSetRequest(
        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Integer setNumber,
        Double weight,
        Integer reps,
        String notes
) {}

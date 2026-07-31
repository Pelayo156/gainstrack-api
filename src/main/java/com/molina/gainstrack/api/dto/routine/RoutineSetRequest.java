package com.molina.gainstrack.api.dto.routine;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de entrada para crear o actualizar un set de un ejercicio en una rutina.
 * En creación solo setNumber es obligatorio — weight, reps y notes son
 * opcionales y se insertan con el valor recibido, incluyendo null si el
 * usuario aún no los conoce.
 * En actualización todos los campos son opcionales. setNumber usa COALESCE
 * y se preserva si no se envía; weight, reps y notes se sobrescriben con
 * el valor recibido — enviar null los deja vacíos intencionalmente, ya que
 * el usuario puede no conocer aún el peso o las reps.
 *
 * @param setNumber número de serie dentro del ejercicio — obligatorio en creación
 * @param weight    peso en kilogramos — opcional, null lo deja sin definir
 * @param reps      repeticiones — opcional, null las deja sin definir
 * @param notes     notas opcionales del set — opcional, null las deja sin definir
 */
public record RoutineSetRequest(
        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Integer setNumber,
        Double weight,
        Integer reps,
        String notes
) {}

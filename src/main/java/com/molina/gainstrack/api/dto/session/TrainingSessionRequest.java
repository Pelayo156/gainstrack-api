package com.molina.gainstrack.api.dto.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de entrada para la creación de una sesión de entrenamiento.
 * No incluye notes — las notas de la sesión creada se heredan de la
 * última sesión del usuario para la misma rutina y gimnasio, o de la
 * plantilla de la rutina si no existe historial previo.
 *
 * @param gymId     id del gimnasio donde se realiza la sesión — opcional, puede ser null
 * @param routineId id de la rutina a ejecutar — obligatorio
 */
public record TrainingSessionRequest(
        Long gymId,
        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Long routineId
) {}

package com.molina.gainstrack.api.dto.session;

/**
 * DTO de entrada para la creación de una sesión de entrenamiento.
 *
 * @param gymId     id del gimnasio donde se realiza la sesión — opcional, puede ser null
 * @param routineId id de la rutina a ejecutar — obligatorio
 * @param notes     título o descripción de la sesión ingresado por el usuario al finalizar
 */
public record TrainingSessionRequest(Long gymId,
                                     Long routineId,
                                     String notes) {}

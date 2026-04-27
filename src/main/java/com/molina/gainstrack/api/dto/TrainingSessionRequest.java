package com.molina.gainstrack.api.dto;

/**
 * DTO de entrada para la creación de una sesión de entrenamiento.
 *
 * @param gymId     id del gimnasio donde se realiza la sesión
 * @param routineId id de la rutina a precargar como ejercicios — opcional, puede ser null
 */
public record TrainingSessionRequest(Long gymId,
                                     Long routineId) {}

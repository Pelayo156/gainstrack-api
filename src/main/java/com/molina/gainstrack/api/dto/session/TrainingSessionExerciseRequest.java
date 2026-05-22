package com.molina.gainstrack.api.dto.session;

/**
 * DTO de entrada para agregar un ejercicio a una sesión.
 *
 * @param exerciseId id del ejercicio del catálogo a agregar
 * @param orderIndex posición del ejercicio dentro de la sesión
 */
public record TrainingSessionExerciseRequest(Long exerciseId,
                                             Integer orderIndex,
                                             String notes) {}

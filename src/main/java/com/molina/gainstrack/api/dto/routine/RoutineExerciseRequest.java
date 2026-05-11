package com.molina.gainstrack.api.dto.routine;

/**
 * DTO de entrada para agregar un ejercicio a una rutina.
 *
 * @param exerciseId id del ejercicio del catálogo a agregar
 * @param orderIndex posición del ejercicio dentro de la rutina
 */
public record RoutineExerciseRequest(Long exerciseId,
                                     Integer orderIndex,
                                     String notes) {}

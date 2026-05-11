package com.molina.gainstrack.api.dto.routine;

/**
 * DTO de entrada para actualizar un ejercicio dentro de una rutina.
 * Ambos campos son opcionales — COALESCE preserva los no enviados.
 *
 * @param orderIndex nueva posición del ejercicio — opcional
 * @param notes      nuevas notas del ejercicio — opcional
 */
public record RoutineExerciseUpdateRequest(Integer orderIndex,
                                           String notes) {}

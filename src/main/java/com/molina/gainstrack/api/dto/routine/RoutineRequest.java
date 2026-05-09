package com.molina.gainstrack.api.dto.routine;

/**
 * DTO de entrada para crear o actualizar una rutina.
 * Ambos campos son opcionales en PATCH — solo se actualizan los enviados.
 *
 * @param name  nombre de la rutina
 * @param notes notas opcionales de la rutina
 */
public record RoutineRequest(String name,
                             String notes) {
}

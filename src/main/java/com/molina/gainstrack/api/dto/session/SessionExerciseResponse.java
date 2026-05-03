package com.molina.gainstrack.api.dto.session;

import com.molina.gainstrack.api.dto.exercise.ExerciseResponse;

import java.util.List;

/**
 * DTO de salida que representa un ejercicio dentro de una sesión.
 * Contiene la referencia al ejercicio del catálogo y la lista de sets realizados.
 *
 * @param id         identificador único del session_exercise
 * @param orderIndex orden del ejercicio dentro de la sesión
 * @param notes      notas opcionales del ejercicio en esta sesión
 * @param exercise   datos del ejercicio del catálogo
 * @param sets       lista de sets realizados en este ejercicio
 */
public record SessionExerciseResponse(Long id,
                                      Integer orderIndex,
                                      String notes,
                                      ExerciseResponse exercise,
                                      List<SetResponse> sets) {
}

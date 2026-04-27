package com.molina.gainstrack.api.dto;

/**
 * DTO de salida que representa un set dentro de un ejercicio de sesión.
 *
 * @param id        identificador único del set
 * @param setNumber número de serie dentro del ejercicio
 * @param weight    peso utilizado en kilogramos
 * @param reps      repeticiones realizadas
 * @param notes     notas opcionales del set
 */
public record SetResponse(Long id,
                          Integer setNumber,
                          Double weight,
                          Integer reps,
                          String notes) {
}

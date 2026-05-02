package com.molina.gainstrack.api.dto;

/**
 * DTO de salida que representa un gimnasio registrado.
 *
 * @param id        identificador único del gimnasio
 * @param name      nombre o apodo del gimnasio
 */
public record GymResponse(Long id, String name) {}

package com.molina.gainstrack.api.dto;

/**
 * DTO de salida que representa un gimnasio registrado.
 *
 * @param id        identificador único del gimnasio
 * @param userId    id del usuario propietario
 * @param name      nombre o apodo del gimnasio
 * @param isPrimary indica si es el gimnasio principal del usuario
 */
public record GymResponse(Long id, Long userId, String name, Boolean isPrimary) {}

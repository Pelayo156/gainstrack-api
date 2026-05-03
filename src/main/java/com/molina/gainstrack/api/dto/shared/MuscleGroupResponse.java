package com.molina.gainstrack.api.dto.shared;

/**
 * DTO de salida que representa un grupo muscular del catálogo global.
 *
 * @param id   identificador único del grupo muscular
 * @param name nombre del grupo muscular
 */
public record MuscleGroupResponse(Long id, String name) {}

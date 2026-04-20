package com.molina.gainstrack.api.dto;

/**
 * Respuesta enviada al cliente tras un login o registro exitoso.
 *
 * @param token JWT generado para el usuario autenticado
 */
public record AuthResponse(String token) {}

package com.molina.gainstrack.api.dto;

/**
 * Datos recibidos del cliente para registro o login.
 *
 * @param email    correo electrónico del usuario
 * @param password contraseña en texto plano — se hashea antes de persistir
 */
public record AuthRequest(String email, String password) {}

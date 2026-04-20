package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.AuthRequest;
import com.molina.gainstrack.api.dto.AuthResponse;
import com.molina.gainstrack.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller que expone los endpoints públicos de autenticación.
 * Estos endpoints no requieren JWT — son el punto de entrada al sistema.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * @param authService servicio con la lógica de autenticación
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registra un nuevo usuario y devuelve un JWT listo para usar.
     *
     * @param request body con email y contraseña del nuevo usuario
     * @return 201 Created con el JWT generado
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    /**
     * Autentica un usuario existente y devuelve un JWT.
     *
     * @param request body con email y contraseña del usuario
     * @return 200 OK con el JWT generado
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

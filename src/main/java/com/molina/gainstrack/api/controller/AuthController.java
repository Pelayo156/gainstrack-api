package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.auth.AuthResponse;
import com.molina.gainstrack.api.dto.auth.LoginRequest;
import com.molina.gainstrack.api.dto.auth.RegisterRequest;
import com.molina.gainstrack.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    /**
     * Autentica un usuario existente y devuelve un JWT.
     *
     * @param request body con email y contraseña del usuario
     * @return 200 OK con el JWT generado
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

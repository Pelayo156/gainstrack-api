package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.config.JwtService;
import com.molina.gainstrack.api.dto.auth.AuthRequest;
import com.molina.gainstrack.api.dto.auth.AuthResponse;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio que maneja la lógica de negocio de autenticación.
 * Coordina el registro de nuevos usuarios y la validación
 * de credenciales en el proceso de login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoutineService routineService;

    /**
     * @param userRepository        repositorio de usuarios
     * @param passwordEncoder       encoder BCrypt para hashear contraseñas
     * @param jwtService            servicio para generar tokens JWT
     * @param authenticationManager gestor de autenticación de Spring Security
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RoutineService routineService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.routineService = routineService;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Hashea la contraseña antes de persistirla y genera un JWT
     * para que el usuario quede autenticado inmediatamente.
     *
     * @param request datos de registro con email y contraseña en texto plano
     * @return AuthResponse con el JWT generado
     */
    @Transactional
    public AuthResponse register(AuthRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        userRepository.save(request.email(), hashedPassword);

        // Se crea rutina libre por defecto a usuario registrado
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new RuntimeException("Usuario no encontrado tras registro."));
        this.routineService.saveFree(user.getId());

        String token = jwtService.generateToken(request.email());
        return new AuthResponse(token);
    }

    /**
     * Autentica un usuario verificando sus credenciales contra la base de datos.
     * Spring Security compara la contraseña enviada contra el hash almacenado.
     *
     * @param request datos de login con email y contraseña en texto plano
     * @return AuthResponse con el JWT generado
     * @throws org.springframework.security.core.AuthenticationException
     *         si las credenciales son incorrectas
     */
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(),
                                                                                   request.password()));

        String token = jwtService.generateToken(request.email());
        return new AuthResponse(token);
    }
}

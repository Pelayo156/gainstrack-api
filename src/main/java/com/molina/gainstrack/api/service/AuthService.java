package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.config.JwtService;
import com.molina.gainstrack.api.dto.auth.AuthResponse;
import com.molina.gainstrack.api.dto.auth.LoginRequest;
import com.molina.gainstrack.api.dto.auth.RegisterRequest;
import com.molina.gainstrack.api.repository.RoutineRepository;
import com.molina.gainstrack.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que maneja la lógica de negocio de autenticación.
 * Coordina el registro de nuevos usuarios y la validación
 * de credenciales en el proceso de login.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoutineRepository routineRepository;

    /**
     * @param userRepository        repositorio de usuarios
     * @param passwordEncoder       encoder BCrypt para hashear contraseñas
     * @param jwtService            servicio para generar tokens JWT
     * @param authenticationManager gestor de autenticación de Spring Security
     * @param routineRepository     repositorio de rutinas para crear rutina libre
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       RoutineRepository routineRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.routineRepository = routineRepository;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Hashea la contraseña, crea el usuario, genera su rutina libre automáticamente
     * y retorna un JWT listo para usar.
     * La anotación @Transactional garantiza que si la creación de la rutina libre
     * falla, la creación del usuario también se revierte.
     *
     * @param request datos de registro con email y contraseña en texto plano
     * @return AuthResponse con el JWT generado
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        Long userId = userRepository.save(request.email(), hashedPassword);
        this.routineRepository.saveFree(userId);
        String token = jwtService.generateToken(request.email());
        LOG.info("Nuevo usuario registrado — userId: {}", userId);
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
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(),
                                                                                   request.password()
        ));
        String token = jwtService.generateToken(request.email());
        LOG.info("Login exitoso — userId obtenido por email");
        return new AuthResponse(token);
    }
}
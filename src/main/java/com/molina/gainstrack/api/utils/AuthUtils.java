package com.molina.gainstrack.api.utils;

import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Clase utilitaria que centraliza la obtención del usuario autenticado
 * desde el contexto de seguridad de Spring.
 * Evita duplicar esta lógica en cada Service de la aplicación.
 */
@Component
public class AuthUtils {

    private final UserRepository userRepository;

    /**
     * @param userRepository repositorio para buscar el usuario en la base de datos
     */
    public AuthUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Obtiene el usuario autenticado desde el contexto de seguridad.
     *
     * @return User correspondiente al JWT en curso
     * @throws RuntimeException si el usuario no existe en la base de datos
     */
    public User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}

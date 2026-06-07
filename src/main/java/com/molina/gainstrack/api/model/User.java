package com.molina.gainstrack.api.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entidad que representa un usuario del sistema.
 * Implementa UserDetails para integrarse con Spring Security,
 * permitiendo que sea usado directamente en el proceso de autenticación.
 * Soporta autenticación por email/contraseña y por Google OAuth.
 * En usuarios de Google, passwordHash es null.
 * En usuarios de email/contraseña, googleId es null.
 * Ambos campos pueden tener valor si las cuentas están vinculadas.
 */
public class User implements UserDetails {

    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private String googleId;

    /**
     * @param id           identificador único del usuario
     * @param name         nombre del usuario
     * @param email        correo electrónico del usuario
     * @param passwordHash hash BCrypt de la contraseña — null para usuarios de Google
     * @param googleId     identificador único de Google — null para usuarios de email/contraseña
     */
    public User(Long id,
                String name,
                String email,
                String passwordHash,
                String googleId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleId = googleId;
    }

    /**
     * Devuelve los roles del usuario.
     * Por ahora todos los usuarios tienen el mismo rol base.
     *
     * @return colección con el rol USER
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Devuelve la contraseña hasheada.
     * Spring Security la usa para verificar credenciales en el login.
     * Puede ser null para usuarios autenticados con Google.
     *
     * @return hash BCrypt de la contraseña o null
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Devuelve el identificador único del usuario para Spring Security.
     * Usamos email en vez de username tradicional.
     *
     * @return email del usuario
     */
    @Override
    public String getUsername() {
        return email;
    }

    public String getName() { return name; }

    public Long getId() { return id; }

    public String getEmail() { return email; }

    public String getGoogleId() { return googleId; }
}

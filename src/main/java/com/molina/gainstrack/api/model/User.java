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
 */
public class User implements UserDetails {

    private Long id;
    private String email;
    private String passwordHash;

    public User(Long id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
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
     *
     * @return hash BCrypt de la contraseña
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

    public Long getId() { return id; }

    public String getEmail() { return email; }
}

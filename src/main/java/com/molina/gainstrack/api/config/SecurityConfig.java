package com.molina.gainstrack.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 * Define las reglas de acceso, la política de sesiones
 * y registra el filtro JWT en la cadena de seguridad.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * @param jwtFilter filtro JWT a registrar en la cadena de seguridad
     */
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Define la cadena de filtros de seguridad HTTP.
     * Deshabilita CSRF, configura sesiones stateless y registra
     * el filtro JWT antes del filtro de autenticación de Spring.
     *
     * @param http objeto de configuración de seguridad HTTP
     * @return SecurityFilterChain configurada
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**")
                                                                             .permitAll()
                                                                             .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Define el encoder de contraseñas usando BCrypt.
     * Nunca se almacenan contraseñas en texto plano.
     *
     * @return PasswordEncoder BCrypt para hashear contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el AuthenticationManager como bean del contenedor.
     * AuthService lo necesita para delegar la verificación
     * de credenciales a Spring Security.
     *
     * @param config configuración de autenticación de Spring
     * @return AuthenticationManager configurado
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws
                                                                                           Exception {
        return config.getAuthenticationManager();
    }
}

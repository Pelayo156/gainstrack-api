package com.molina.gainstrack.api.config;

import com.molina.gainstrack.api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param userRepository repositorio para consultar usuarios en MySQL
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carga los datos de un usuario por su email.
     * Spring Security llama a este método durante el proceso de login
     * para verificar que el usuario existe en la base de datos.
     *
     * @param email correo electrónico del usuario a buscar
     * @return UserDetails objeto con la información de seguridad del usuario
     * @throws UsernameNotFoundException si no existe un usuario con ese email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                             .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

    }
}

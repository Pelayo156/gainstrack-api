package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.gym.GymRequest;
import com.molina.gainstrack.api.dto.gym.GymResponse;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.GymRepository;
import com.molina.gainstrack.api.utils.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que maneja la lógica de negocio para la gestión de gimnasios.
 * Obtiene el usuario autenticado desde el contexto de seguridad de Spring
 * para garantizar que cada operación esté acotada al usuario en sesión.
 */
@Service
public class GymService {

    private final GymRepository gymRepository;
    private final AuthUtils authUtils;

    /**
     * @param gymRepository  repositorio de acceso a datos de gimnasios
     * @param authUtils componente de acceso a metodo para obtener usuario
     */
    public GymService(GymRepository gymRepository, AuthUtils authUtils) {
        this.gymRepository = gymRepository;
        this.authUtils = authUtils;
    }

    /**
     * Crea un nuevo gimnasio para el usuario autenticado.
     *
     * @param request datos del gimnasio a crear
     * @return GymResponse con los datos del gimnasio creado
     * @throws RuntimeException si el usuario autenticado no existe en la base de datos
     */
    public GymResponse save(GymRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        return gymRepository.save(user.getId(), request.name());
    }

    /**
     * Retorna todos los gimnasios del usuario autenticado.
     *
     * @return lista de gimnasios del usuario en sesión
     * @throws RuntimeException si el usuario autenticado no existe en la base de datos
     */
    public List<GymResponse> findAll() {
        User user = this.authUtils.getAuthenticatedUser();
        return gymRepository.findAll(user.getId());
    }

    /**
     * Elimina un gimnasio por su id.
     * Por el CASCADE del modelo relacional, se eliminarán también
     * todas las sesiones asociadas a este gimnasio.
     *
     * @param id id del gimnasio a eliminar
     */
    public void deleteById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        gymRepository.deleteById(id, user.getId());
    }
}

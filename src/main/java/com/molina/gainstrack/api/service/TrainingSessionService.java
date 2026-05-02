package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.TrainingSessionRequest;
import com.molina.gainstrack.api.dto.TrainingSessionSummaryResponse;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.TrainingSessionRepository;
import com.molina.gainstrack.api.utils.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que maneja la lógica de negocio para las sesiones de entrenamiento.
 * Obtiene el usuario autenticado desde AuthUtils para garantizar
 * que cada operación esté acotada al usuario en sesión.
 */
@Service
public class TrainingSessionService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final AuthUtils authUtils;

    /**
     * @param trainingSessionRepository repositorio de acceso a datos de sesiones
     * @param authUtils                 utilidad para obtener el usuario autenticado
     */
    public TrainingSessionService(TrainingSessionRepository trainingSessionRepository,
                                  AuthUtils authUtils) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.authUtils = authUtils;
    }

    /**
     * Retorna el resumen de todas las sesiones del usuario autenticado.
     * Cada sesión incluye su gimnasio asociado pero no el detalle
     * de ejercicios ni sets.
     *
     * @return lista de sesiones de entrenamiento del usuario en sesión
     */
    public List<TrainingSessionSummaryResponse> findAll() {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.findAll(user.getId());
    }

    /**
     * Crea una nueva sesión de entrenamiento para el usuario autenticado.
     * Si se proporciona una rutina, precarga sus ejercicios en la sesión.
     * La fecha se asigna automáticamente como la fecha actual del servidor.
     *
     * @param request datos de la sesión — gymId obligatorio, routineId opcional
     * @return TrainingSessionSummaryResponse con los datos de la sesión creada
     */

    public TrainingSessionSummaryResponse save(TrainingSessionRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.save(user.getId(),
                                                   request.gymId(),
                                                   request.routineId());
    }

    /**
     * Elimina una sesión de entrenamiento del usuario autenticado.
     * Por el CASCADE del modelo relacional, se eliminarán también
     * todos los ejercicios y sets asociados a esta sesión.
     *
     * @param id id de la sesión a eliminar
     */
    public void deleteById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        trainingSessionRepository.deleteById(id, user.getId());
    }
}

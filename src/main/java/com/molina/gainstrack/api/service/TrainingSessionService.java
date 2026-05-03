package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.session.TrainingSessionDetailResponse;
import com.molina.gainstrack.api.dto.session.TrainingSessionRequest;
import com.molina.gainstrack.api.dto.session.TrainingSessionSummaryResponse;
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
     * Copia ejercicios y sets desde la rutina indicada como punto de partida.
     * La fecha se asigna automáticamente como la fecha actual del servidor.
     *
     * @param request datos de la sesión — routineId obligatorio, gymId y notes opcionales
     * @return TrainingSessionDetailResponse con el detalle completo de la sesión creada
     */
    public TrainingSessionDetailResponse save(TrainingSessionRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.save(user.getId(),
                                                   request.gymId(),
                                                   request.routineId(),
                                                   request.notes());
    }

    /**
     * Retorna el detalle completo de una sesión del usuario autenticado.
     * Incluye ejercicios realizados con sus sets y pesos registrados.
     *
     * @param id id de la sesión a consultar
     * @return TrainingSessionDetailResponse con ejercicios y sets anidados
     */
    public TrainingSessionDetailResponse findById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.trainingSessionRepository.findById(id, user.getId());
    }

    /**
     * Elimina una sesión de entrenamiento del usuario autenticado.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los ejercicios y sets asociados a esta sesión.
     *
     * @param id id de la sesión a eliminar
     */
    public void deleteById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        trainingSessionRepository.deleteById(id, user.getId());
    }
}

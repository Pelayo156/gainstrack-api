package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.routine.RoutineDetailResponse;
import com.molina.gainstrack.api.dto.routine.RoutineRequest;
import com.molina.gainstrack.api.dto.routine.RoutineSummaryResponse;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.RoutineRepository;
import com.molina.gainstrack.api.utils.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que maneja la lógica de negocio para la gestión de rutinas.
 * Obtiene el usuario autenticado desde AuthUtils para garantizar
 * que cada operación esté acotada al usuario en sesión.
 */
@Service
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final AuthUtils authUtils;

    /**
     * @param routineRepository repositorio de acceso a datos de rutinas
     * @param authUtils         utilidad para obtener el usuario autenticado
     */
    public RoutineService(RoutineRepository routineRepository,
                          AuthUtils authUtils) {
        this.routineRepository = routineRepository;
        this.authUtils = authUtils;
    }

    /**
     * Retorna el resumen de todas las rutinas del usuario autenticado.
     * No incluye ejercicios ni sets.
     *
     * @return lista de rutinas del usuario en sesión
     */
    public List<RoutineSummaryResponse> findAll() {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.findAll(user.getId());
    }

    /**
     * Retorna el detalle completo de una rutina del usuario autenticado.
     * Incluye ejercicios con sus sets de referencia.
     *
     * @param id id de la rutina a consultar
     * @return RoutineDetailResponse con ejercicios y sets anidados
     */
    public RoutineDetailResponse findById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.findById(id,
                                               user.getId());
    }

    /**
     * Crea una nueva rutina para el usuario autenticado.
     *
     * @param request datos de la rutina — nombre obligatorio, notas opcionales
     * @return RoutineSummaryResponse con los datos de la rutina creada
     */
    public RoutineSummaryResponse save(RoutineRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.save(user.getId(),
                                           request.name(),
                                           request.notes());
    }

    /**
     * Actualiza nombre y/o notas de una rutina del usuario autenticado.
     * Solo actualiza los campos enviados — campos null conservan su valor actual.
     *
     * @param id      id de la rutina a actualizar
     * @param request campos a actualizar — name y/o notes, ambos opcionales
     */
    public void update(Long id, RoutineRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        this.routineRepository.update(id,
                                      user.getId(),
                                      request.name(),
                                      request.notes());
    }

    /**
     * Elimina una rutina del usuario autenticado.
     * La rutina especial is_free no se puede eliminar.
     * Por el CASCADE del modelo relacional, se eliminan también
     * todos los ejercicios, sets y sesiones asociadas a esta rutina.
     *
     * @param id id de la rutina a eliminar
     * @throws RuntimeException si se intenta eliminar la rutina libre
     */
    public void deleteById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id, user.getId());

        if (routine.isFree()) {
            throw new RuntimeException("La rutina libre no se puede eliminar");
        }

        this.routineRepository.deleteById(id, user.getId());
    }

    public RoutineDetailResponse saveExercise(Long id, Long exerciseId) {
        User user = this.authUtils.getAuthenticatedUser();
        RoutineDetailResponse routine = this.routineRepository.findById(id, user.getId());

        if (routine == null) return null;

        return this.routineRepository.saveExercise(id,
                                                   exerciseId,
                                                   routine.exercises().size(),
                                                   user.getId());
    }
}

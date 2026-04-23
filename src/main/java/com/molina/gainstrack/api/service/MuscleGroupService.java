package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.MuscleGroupResponse;
import com.molina.gainstrack.api.repository.MuscleGroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que maneja la lógica de negocio para los grupos musculares.
 * Al ser datos de catálogo global, no requiere validación de usuario.
 */
@Service
public class MuscleGroupService {

    /**
     * @param muscleGroupRepository repositorio de acceso a datos de grupos musculares
     */
    private final MuscleGroupRepository muscleGroupRepository;

    public MuscleGroupService(MuscleGroupRepository muscleGroupRepository) {
        this.muscleGroupRepository = muscleGroupRepository;
    }

    /**
     * Retorna todos los grupos musculares disponibles en el sistema.
     *
     * @return lista completa de grupos musculares
     */
    public List<MuscleGroupResponse> findAll() {
        return muscleGroupRepository.findAll();
    }
}

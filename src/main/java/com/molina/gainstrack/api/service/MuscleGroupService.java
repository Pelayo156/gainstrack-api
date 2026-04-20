package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.MuscleGroupResponse;
import com.molina.gainstrack.api.repository.MuscleGroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MuscleGroupService {

    private final MuscleGroupRepository muscleGroupRepository;

    public MuscleGroupService(MuscleGroupRepository muscleGroupRepository) {
        this.muscleGroupRepository = muscleGroupRepository;
    }

    public List<MuscleGroupResponse> findAll() {
        return muscleGroupRepository.findAll();
    }
}

package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.routine.RoutineDetailResponse;
import com.molina.gainstrack.api.dto.routine.RoutineRequest;
import com.molina.gainstrack.api.dto.routine.RoutineSummaryResponse;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.RoutineRepository;
import com.molina.gainstrack.api.utils.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final AuthUtils authUtils;

    public RoutineService(RoutineRepository routineRepository,
                          AuthUtils authUtils) {
        this.routineRepository = routineRepository;
        this.authUtils = authUtils;
    }

    public List<RoutineSummaryResponse> findAll() {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.findAll(user.getId());
    }

    public RoutineDetailResponse findById(Long id) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.findById(id,
                                               user.getId());
    }

    public RoutineSummaryResponse save(RoutineRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        return this.routineRepository.save(user.getId(),
                                           request.name(),
                                           request.notes());
    }

    public void update(Long id, RoutineRequest request) {
        User user = this.authUtils.getAuthenticatedUser();
        this.routineRepository.update(id,
                                      user.getId(),
                                      request.name(),
                                      request.notes());
    }
}

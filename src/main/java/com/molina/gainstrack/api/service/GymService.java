package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.dto.GymResponse;
import com.molina.gainstrack.api.model.User;
import com.molina.gainstrack.api.repository.GymRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class GymService {

    private final GymRepository gymRepository;
    private final AuthService authService;

    public GymService(GymRepository gymRepository, AuthService authService) {
        this.gymRepository = gymRepository;
        this.authService = authService;
    }

    public GymResponse save(String name) {
        if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = authService.findByEmail(userEmail)
                               .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return gymRepository.save(userId, name);
    }

    public List<GymResponse> findAll(Long userId) {
        return gymRepository.findAll(userId);
    }

    public void setPrimary(Long id, Long userId) {
        gymRepository.setPrimary(id, userId);
    }

    public void deleteById(Long id) {
        gymRepository.deleteById(id);
    }
}

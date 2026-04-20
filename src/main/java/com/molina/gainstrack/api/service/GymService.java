package com.molina.gainstrack.api.service;

import com.molina.gainstrack.api.repository.GymRepository;
import org.springframework.stereotype.Service;

@Service
public class GymService {

    private final GymRepository gymRepository;

    public GymService(GymRepository gymRepository) { this.gymRepository = gymRepository; }
}

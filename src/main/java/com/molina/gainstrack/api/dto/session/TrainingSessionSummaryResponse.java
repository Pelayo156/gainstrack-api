package com.molina.gainstrack.api.dto.session;

import com.molina.gainstrack.api.dto.gym.GymResponse;

import java.time.LocalDate;

public record TrainingSessionSummaryResponse(Long id,
                                             GymResponse gym,
                                             LocalDate sessionDate,
                                             String notes) {}

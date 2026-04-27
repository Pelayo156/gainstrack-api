package com.molina.gainstrack.api.dto;

import java.time.LocalDate;

public record TrainingSessionSummaryResponse(Long id,
                                             GymResponse gym,
                                             LocalDate sessionDate,
                                             String notes) {}

package com.molina.gainstrack.api.dto.routine;

import java.time.LocalDate;

public record RoutineSummaryResponse(Long id,
                                     String name,
                                     LocalDate createdAt,
                                     String notes) {}

package com.molina.gainstrack.api.dto.routine;

import java.time.LocalDate;
import java.util.List;

public record RoutineDetailResponse(Long id,
                                    String name,
                                    LocalDate createdAt,
                                    String notes,
                                    List<RoutineExerciseResponse> exercises) {}

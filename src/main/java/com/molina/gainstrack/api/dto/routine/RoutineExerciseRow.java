package com.molina.gainstrack.api.dto.routine;

public record RoutineExerciseRow(Long id,
                                 Long exerciseId,
                                 Integer orderIndex,
                                 String notes) {}

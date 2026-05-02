package com.molina.gainstrack.api.dto;

public record RoutineExerciseRow(Long id,
                                 Long exerciseId,
                                 Integer orderIndex,
                                 String notes) {}

package com.molina.gainstrack.api.dto;

public record ExerciseResponse(Long id,
                               String name,
                               MuscleGroupResponse muscleGroup,
                               Long userId,
                               Boolean isPredefined) {
}

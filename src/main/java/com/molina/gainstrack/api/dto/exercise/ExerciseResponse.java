package com.molina.gainstrack.api.dto.exercise;

import com.molina.gainstrack.api.dto.shared.MuscleGroupResponse;

public record ExerciseResponse(Long id,
                               String name,
                               MuscleGroupResponse muscleGroup,
                               Long userId,
                               Boolean isPredefined) {
}

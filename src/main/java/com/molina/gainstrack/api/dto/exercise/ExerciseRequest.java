package com.molina.gainstrack.api.dto.exercise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.size.max}")
        String name,

        @NotNull(message = "{field.required}")
        @Positive(message = "{field.positive}")
        Long muscleGroupId
) {}

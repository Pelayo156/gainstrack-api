package com.molina.gainstrack.api.dto.gym;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para la creación de un gimnasio.
 *
 * @param name nombre o apodo del gimnasio
 */
public record GymRequest(
        @NotBlank(message = "{field.required}")
        @Size(max = 150, message = "{field.size.max}")
        String name
) {}

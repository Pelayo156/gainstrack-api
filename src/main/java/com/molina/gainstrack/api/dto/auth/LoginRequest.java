package com.molina.gainstrack.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{field.required}")
        String email,

        @NotBlank(message = "{field.required}")
        String password
) {}

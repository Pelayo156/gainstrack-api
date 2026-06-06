package com.molina.gainstrack.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{field.email.required}")
        String email,

        @NotBlank(message = "{field.password.required}")
        String password
) {}

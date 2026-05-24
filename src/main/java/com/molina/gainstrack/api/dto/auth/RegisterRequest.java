package com.molina.gainstrack.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{field.required}")
        @Email(message = "{field.email.invalid}")
        String email,

        @NotBlank(message = "{field.required}")
        @Size(min = 6, message = "{field.size.min}")
        String password
) {}

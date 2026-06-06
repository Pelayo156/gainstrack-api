package com.molina.gainstrack.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "{field.email.required}")
        @Email(message = "{field.email.invalid}")
        String email,

        @NotBlank(message = "{field.password.required}")
        @Size(min = 6, message = "{field.password.size.min}")
        String password
) {}

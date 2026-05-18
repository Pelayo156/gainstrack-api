package com.molina.gainstrack.api.dto.shared;

import java.time.LocalDateTime;

public record ErrorResponse(int status,
                            String error,
                            String message,
                            LocalDateTime timestamp) {
}

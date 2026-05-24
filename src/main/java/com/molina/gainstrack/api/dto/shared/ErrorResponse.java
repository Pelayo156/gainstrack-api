package com.molina.gainstrack.api.dto.shared;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de salida para respuestas de error de la API.
 * El campo errors solo se popula en errores de validación de campos —
 * en los demás casos viene null.
 *
 * @param status    código HTTP del error
 * @param error     nombre del status HTTP
 * @param message   mensaje descriptivo del error
 * @param errors    mapa de errores por campo — solo en errores de validación
 * @param timestamp fecha y hora en que ocurrió el error
 */

public record ErrorResponse(int status,
                            String error,
                            String message,
                            Map<String, String> errors,
                            LocalDateTime timestamp) {
}

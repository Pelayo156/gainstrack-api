package com.molina.gainstrack.api.dto.session;

import com.molina.gainstrack.api.dto.gym.GymResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de salida con el detalle completo de una sesión de entrenamiento.
 * Incluye el gym, los ejercicios realizados y los sets de cada ejercicio.
 *
 * @param id          identificador único de la sesión
 * @param gym         gimnasio donde se realizó la sesión
 * @param sessionDate fecha de la sesión
 * @param notes       notas opcionales de la sesión
 * @param exercises   lista de ejercicios realizados con sus sets
 */
public record TrainingSessionDetailResponse(Long id,
                                            GymResponse gym,
                                            LocalDate sessionDate,
                                            String notes,
                                            List<TrainingSessionExerciseResponse> exercises) {}

package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.GymResponse;
import com.molina.gainstrack.api.dto.TrainingSessionSummaryResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.List;

/**
 * Repositorio de acceso a datos para la tabla training_sessions.
 * Ejecuta SQL puro mediante JdbcClient, sin ORM.
 * Todas las operaciones están acotadas al usuario propietario
 * para garantizar aislamiento de datos entre usuarios.
 */
@Repository
public class TrainingSessionRepository {

    /**
     * @param jdbcClient cliente JDBC para ejecutar consultas SQL
     */
    private final JdbcClient jdbcClient;

    public TrainingSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Retorna el resumen de todas las sesiones de entrenamiento del usuario.
     * Incluye los datos del gimnasio asociado a cada sesión mediante JOIN.
     * No incluye ejercicios ni sets — usar findById para el detalle completo.
     *
     * @param userId id del usuario autenticado
     * @return lista de sesiones con sus datos de cabecera y gimnasio
     */
    public List<TrainingSessionSummaryResponse> findAll(Long userId) {
        return jdbcClient.sql("SELECT ts.id AS training_session_id, " +
                                      "g.id AS gym_id, " +
                                      "g.name AS gym_name, " +
                                      "g.is_primary AS gym_is_primary, " +
                                      "ts.session_date AS training_session_date, " +
                                      "ts.notes AS training_session_notes " +
                              "FROM training_sessions ts " +
                              "JOIN gyms g ON ts.gym_id = g.id " +
                              "WHERE ts.user_id = :userId")
                .param("userId", userId)
                .query((rs, rowNum) -> new TrainingSessionSummaryResponse(
                        rs.getLong("training_session_id"),
                        new GymResponse(
                                rs.getLong("gym_id"),
                                rs.getString("gym_name"),
                                rs.getBoolean("gym_is_primary")
                        ),
                        rs.getDate("training_session_date")
                          .toLocalDate(),
                        rs.getString("training_session_notes")
                ))
                .list();
    }

    public TrainingSessionSummaryResponse save(Long userId, Long gymId, Long routineId) {

        if (routineId == null) {
            this.jdbcClient.sql("INSERT INTO training_sessions (user_id, gym_id, session_date) " +
                                "VALUES (:userId, :gymId, NOW())")
                    .param("userId", userId)
                    .param("gymId", gymId)
                    .update();
        }

        return jdbcClient.sql("SELECT ts.id AS training_session_id, " +
                              "g.id AS gym_id, " +
                              "g.name AS gym_name, " +
                              "g.is_primary AS gym_is_primary, " +
                              "ts.session_date AS training_session_date, " +
                              "ts.notes AS training_session_notes " +
                              "FROM training_sessions ts " +
                              "JOIN gyms g ON ts.gym_id = g.id " +
                              "WHERE ts.user_id = :userId ORDER BY ts.id DESC LIMIT 1")
                .param("userId", userId)
                .query((rs, rowNum) -> new TrainingSessionSummaryResponse(
                        rs.getLong("training_session_id"),
                        new GymResponse(
                                rs.getLong("gym_id"),
                                rs.getString("gym_name"),
                                rs.getBoolean("gym_is_primary")
                        ),
                        rs.getDate("training_session_date")
                          .toLocalDate(),
                        rs.getString("training_session_notes")
                ))
                .single();
    }
}

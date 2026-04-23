package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.MuscleGroupResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la tabla muscle_groups.
 * Ejecuta SQL puro mediante JdbcClient, sin ORM.
 * Los grupos musculares son datos globales del sistema —
 * no pertenecen a ningún usuario en particular.
 */
@Repository
public class MuscleGroupRepository {
    private final JdbcClient jdbcClient;

    /**
     * @param jdbcClient cliente JDBC para ejecutar consultas SQL
     */
    public MuscleGroupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Repositorio de acceso a datos para la tabla muscle_groups.
     * Ejecuta SQL puro mediante JdbcClient, sin ORM.
     * Los grupos musculares son datos globales del sistema —
     * no pertenecen a ningún usuario en particular.
     */
    public List<MuscleGroupResponse> findAll() {
        return jdbcClient.sql("SELECT id, name FROM muscle_groups")
                         .query(MuscleGroupResponse.class)
                         .list();
    }
}

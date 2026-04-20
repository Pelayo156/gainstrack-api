package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.MuscleGroupResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MuscleGroupRepository {
    private final JdbcClient jdbcClient;

    public MuscleGroupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<MuscleGroupResponse> findAll() {
        return jdbcClient.sql("SELECT id, name FROM muscle_groups")
                         .query(MuscleGroupResponse.class)
                         .list();
    }
}

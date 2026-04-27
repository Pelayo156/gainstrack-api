package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.TrainingSessionSummaryResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TrainingSessionRepository {

    private final JdbcClient jdbcClient;

    public TrainingSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

//    public List<TrainingSessionSummaryResponse> findAll() {
//        return jdbcClient.sql("SELECT ")
//    }
}

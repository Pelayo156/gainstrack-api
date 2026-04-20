package com.molina.gainstrack.api.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class GymRepository {

    private final JdbcClient jdbcClient;

    GymRepository(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }
}

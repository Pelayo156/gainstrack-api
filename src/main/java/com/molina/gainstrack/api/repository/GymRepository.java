package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.GymResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GymRepository {

    private final JdbcClient jdbcClient;

    GymRepository(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public GymResponse save(Long userId, String name) {
        return jdbcClient.sql("INSERT INTO gyms (user_id, name, is_primary) VALUES (:userId, :name, FALSE) RETURNING id, user_id, name, is_primary")
                         .param("userId", userId)
                         .param("name", name)
                         .query(GymResponse.class)
                         .single();
    }

    public List<GymResponse> findAll(Long userId) {
        return jdbcClient.sql("SELECT id, user_id, name, is_primary FROM gyms WHERE user_id = :userId")
                         .param("userId", userId)
                         .query(GymResponse.class)
                         .list();
    }

    public void setPrimary(Long id, Long userId) {
        // Desmarcar actual gimnasio principal
        jdbcClient.sql("UPDATE gyms SET is_primary = FALSE WHERE user_id = :userId AND is_primary = TRUE")
                  .param("userId", userId)
                  .update();

        // Marcar nuevo gimnasio como pricipal
        jdbcClient.sql("UPDATE gyms SET is_primary = TRUE WHERE id = :id AND user_id = :userId")
                  .param("id", id)
                  .param("userId", userId)
                  .update();
    }

    public void deleteById(Long id) {
        jdbcClient.sql("DELETE FROM gyms WHERE id = :id")
                  .param("id", id)
                  .update();
    }
}

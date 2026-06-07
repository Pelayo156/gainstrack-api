package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.model.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la tabla users.
 * Ejecuta SQL puro mediante JdbcClient, sin ORM.
 */
@Repository
public class UserRepository {

    private final JdbcClient jdbcClient;

    /**
     * @param jdbcClient cliente JDBC para ejecutar consultas SQL
     */
    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<User> findByEmail(String email) {
        return jdbcClient.sql("SELECT id, name, email, password_hash, google_id FROM users WHERE email = :email")
                         .param("email", email)
                         .query((rs, rowNum) -> new User(
                                 rs.getLong("id"),
                                 rs.getString("name"),
                                 rs.getString("email"),
                                 rs.getString("password_hash"),
                                 rs.getString("google_id")
                         ))

                         .optional();
    }

    /**
     * Inserta un nuevo usuario en la base de datos.
     * Usa KeyHolder para obtener el id generado automáticamente por MySQL.
     *
     * @param email        correo electrónico del nuevo usuario
     * @param passwordHash contraseña ya hasheada con BCrypt
     * @return id generado para el usuario recién creado
     */
    public Long save(String name, String email, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("INSERT INTO users (name, email, password_hash) VALUES (:name, :email, :passwordHash)")
                  .param("name", name)
                  .param("email", email)
                  .param("passwordHash", passwordHash)
                  .update(keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

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
        return jdbcClient.sql("SELECT id, email, password_hash FROM users WHERE email = :email")
                         .param("email", email)
                         .query(User.class)
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
    public Long save(String email, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("INSERT INTO users (email, password_hash) VALUES (:email, :passwordHash)")
                  .param("email", email)
                  .param("passwordHash", passwordHash)
                  .update(keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

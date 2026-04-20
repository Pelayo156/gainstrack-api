package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.model.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
     * Guarda un nuevo usuario en la base de datos.
     *
     * @param email        correo electrónico del nuevo usuario
     * @param passwordHash contraseña ya hasheada con BCrypt
     */
    public void save(String email, String passwordHash) {
        jdbcClient.sql("INSERT INTO users (email, password_hash) VALUES (:email, :passwordHash)")
                  .param("email", email)
                  .param("passwordHash", passwordHash)
                  .update();
    }
}

package com.molina.gainstrack.api.repository;

import com.molina.gainstrack.api.dto.gym.GymResponse;
import com.molina.gainstrack.api.exception.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la tabla gyms.
 * Ejecuta SQL puro mediante JdbcClient, sin ORM.
 * Todas las operaciones están acotadas al usuario propietario
 * para garantizar aislamiento de datos entre usuarios.
 */
@Repository
public class GymRepository {

    /**
     * @param jdbcClient cliente JDBC para ejecutar consultas SQL
     */
    private final JdbcClient jdbcClient;

    GymRepository(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    /**
     * Inserta un nuevo gimnasio y retorna el registro creado.
     * El gimnasio se crea con is_primary en FALSE por defecto.
     * Se recupera el registro usando ORDER BY id DESC LIMIT 1
     * ya que MySQL no soporta RETURNING como PostgreSQL.
     *
     * @param userId id del usuario propietario del gimnasio
     * @param name   nombre o apodo del gimnasio
     * @return GymResponse con los datos del gimnasio recién creado
     */
    public GymResponse save(Long userId, String name) {
        jdbcClient.sql("INSERT INTO gyms (user_id, name) VALUES (:userId, :name)")
                  .param("userId", userId)
                  .param("name", name)
                  .update();

        return jdbcClient.sql("SELECT id, name FROM gyms WHERE user_id = :userId ORDER BY id DESC LIMIT 1")
                         .param("userId", userId)
                         .query(GymResponse.class)
                         .optional()
                         .orElseThrow(() -> new NotFoundException("Sesión no encontrada"));
    }

    /**
     * Retorna todos los gimnasios registrados por un usuario.
     *
     * @param userId id del usuario propietario
     * @return lista de gimnasios del usuario
     */
    public List<GymResponse> findAll(Long userId) {
        return jdbcClient.sql("SELECT id, name FROM gyms WHERE user_id = :userId")
                         .param("userId", userId)
                         .query(GymResponse.class)
                         .list();
    }

    /**
     * Elimina un gimnasio por su id.
     * Por el CASCADE definido en el DDL, se eliminarán también
     * todas las sesiones de entrenamiento asociadas a este gimnasio.
     *
     * @param id id del gimnasio a eliminar
     * @param userId id del usuario propietario
     */
    public void deleteById(Long id, Long userId) {
        int affectedRows = jdbcClient.sql("DELETE FROM gyms WHERE id = :id AND user_id = :userId")
                                     .param("id", id)
                                     .param("userId", userId)
                                     .update();

        if (affectedRows == 0) {
            throw new NotFoundException("Gimnasio no encontrado");
        }
    }
}

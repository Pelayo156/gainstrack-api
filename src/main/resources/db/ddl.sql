-- ============================================================
-- GainsTrack — DDL Final v2
-- Motor: MySQL 8.0+
-- Orden: de tablas sin dependencias hacia las que dependen de otras
-- ============================================================


-- ------------------------------------------------------------
-- 1. muscle_groups
-- Catálogo global de grupos musculares del sistema.
-- Sin FK. No pertenece a ningún usuario.
-- ------------------------------------------------------------
CREATE TABLE muscle_groups (
    id   INT          NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_muscle_groups      PRIMARY KEY (id),
    CONSTRAINT uq_muscle_groups_name UNIQUE (name)
);


-- ------------------------------------------------------------
-- 2. users
-- Usuarios registrados en el sistema.
-- Sin FK propias. Otras tablas dependen de esta.
-- Al registrarse, se crea automáticamente una rutina libre
-- asociada al usuario desde AuthService.
-- ------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);


-- ------------------------------------------------------------
-- 3. gyms
-- Gimnasios registrados por cada usuario.
-- is_primary: solo un gym por usuario puede ser TRUE.
-- Se controla a nivel de aplicación — MySQL no soporta UNIQUE parcial.
-- CASCADE: al eliminar usuario se eliminan sus gyms.
-- ------------------------------------------------------------
CREATE TABLE gyms (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_gyms      PRIMARY KEY (id),
    CONSTRAINT fk_gyms_user FOREIGN KEY (user_id)
                            REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_gyms_user_id ON gyms(user_id);


-- ------------------------------------------------------------
-- 4. exercises
-- Catálogo de ejercicios — globales y privados por usuario.
-- user_id NULL  = ejercicio global/predefinido — visible para todos,
--                no editable ni eliminable por usuarios.
-- user_id valor = ejercicio privado del usuario — solo él lo ve.
-- deleted_at: soft delete para preservar integridad del historial
--             de sesiones que referencian este ejercicio.
-- ------------------------------------------------------------
CREATE TABLE exercises (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150) NOT NULL,
    muscle_group_id INT          NOT NULL,
    user_id         BIGINT       NULL,
    is_predefined   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP    NULL     DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_exercises        PRIMARY KEY (id),
    CONSTRAINT fk_exercises_muscle FOREIGN KEY (muscle_group_id)
                                   REFERENCES muscle_groups(id),
    CONSTRAINT fk_exercises_user   FOREIGN KEY (user_id)
                                   REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_exercises_user_id ON exercises(user_id);


-- ------------------------------------------------------------
-- 5. routines
-- Plantillas de entrenamiento reutilizables por usuario.
-- Funcionan como carpetas que agrupan sesiones del mismo tipo.
-- is_free: identifica la rutina especial "Sesión Libre" —
--          creada automáticamente al registrar el usuario,
--          no eliminable, solo renombrable.
-- El nombre de la carpeta es siempre igual al nombre de la rutina.
-- CASCADE: al eliminar usuario se eliminan sus rutinas.
-- CASCADE: al eliminar rutina se eliminan sus sesiones asociadas.
-- ------------------------------------------------------------
CREATE TABLE routines (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(150) NOT NULL,
    notes      TEXT         NULL,
    is_free    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_routines      PRIMARY KEY (id),
    CONSTRAINT fk_routines_user FOREIGN KEY (user_id)
                                REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_routines_user_id ON routines(user_id);


-- ------------------------------------------------------------
-- 6. routine_exercises
-- Ejercicios que componen una rutina — define la plantilla.
-- order_index: define el orden de los ejercicios en la rutina.
-- La rutina es inmutable durante la ejecución de una sesión —
-- los cambios en la sesión no afectan la rutina original.
-- CASCADE: al eliminar rutina se eliminan sus ejercicios.
-- ------------------------------------------------------------
CREATE TABLE routine_exercises (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    routine_id  BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    order_index INT    NOT NULL DEFAULT 0,
    notes       TEXT   NULL,
    CONSTRAINT pk_routine_exercises PRIMARY KEY (id),
    CONSTRAINT fk_re_routine        FOREIGN KEY (routine_id)
                                    REFERENCES routines(id) ON DELETE CASCADE,
    CONSTRAINT fk_re_exercise       FOREIGN KEY (exercise_id)
                                    REFERENCES exercises(id)
);

CREATE INDEX idx_routine_exercises_routine_id ON routine_exercises(routine_id);


-- ------------------------------------------------------------
-- 7. routine_sets
-- Series de referencia para cada ejercicio de una rutina.
-- Define peso y repeticiones sugeridos como plantilla.
-- Al ejecutar una rutina, estos sets se copian en la tabla sets
-- como punto de partida editable por el usuario durante la sesión.
-- Las modificaciones en la sesión no alteran estos valores.
-- CASCADE: al eliminar routine_exercise se eliminan sus sets.
-- ------------------------------------------------------------
CREATE TABLE routine_sets (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    routine_exercise_id BIGINT       NOT NULL,
    set_number          INT          NOT NULL,
    weight              DECIMAL(5,2) NOT NULL,
    reps                INT          NOT NULL,
    notes               TEXT         NULL,
    CONSTRAINT pk_routine_sets        PRIMARY KEY (id),
    CONSTRAINT fk_rs_routine_exercise FOREIGN KEY (routine_exercise_id)
                                      REFERENCES routine_exercises(id)
                                      ON DELETE CASCADE
);

CREATE INDEX idx_routine_sets_routine_exercise_id
    ON routine_sets(routine_exercise_id);


-- ------------------------------------------------------------
-- 8. training_sessions
-- Ejecuciones reales de una rutina en una fecha específica.
-- routine_id NOT NULL: toda sesión pertenece a una rutina —
--   sesiones libres se asocian a la rutina especial is_free = TRUE.
-- gym_id NOT NULL: toda sesión debe tener un gym asociado.
-- Las sesiones son inmutables una vez completadas — se controla
-- a nivel de aplicación.
-- Las sesiones se pueden mover entre rutinas actualizando routine_id.
-- CASCADE en routine: al eliminar rutina se eliminan sus sesiones.
-- CASCADE en gym: al eliminar gym se eliminan sus sesiones.
-- CASCADE en user: al eliminar usuario se eliminan sus sesiones.
-- ------------------------------------------------------------
-- ------------------------------------------------------------
CREATE TABLE training_sessions (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    user_id      BIGINT    NOT NULL,
    routine_id   BIGINT    NOT NULL,
    gym_id       BIGINT    NULL,
    session_date DATE      NOT NULL,
    notes        TEXT      NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_training_sessions PRIMARY KEY (id),
    CONSTRAINT fk_ts_user           FOREIGN KEY (user_id)
                                    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_routine        FOREIGN KEY (routine_id)
                                    REFERENCES routines(id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_gym            FOREIGN KEY (gym_id)
                                    REFERENCES gyms(id) ON DELETE SET NULL
);

-- Índice compuesto: optimiza consultas históricas por usuario + fecha
CREATE INDEX idx_ts_user_date ON training_sessions(user_id, session_date);
-- Índice para consultas por rutina: listar sesiones de una carpeta
CREATE INDEX idx_ts_routine_id ON training_sessions(routine_id);


-- ------------------------------------------------------------
-- 9. session_exercises
-- Ejercicios realizados dentro de una sesión de entrenamiento.
-- Copiados desde routine_exercises al ejecutar una rutina,
-- o agregados manualmente en sesiones libres.
-- Sin UNIQUE en (session_id, exercise_id): un ejercicio puede
-- repetirse dentro de la misma sesión.
-- order_index: define el orden de los ejercicios en la sesión.
-- CASCADE en session: al eliminar sesión se eliminan sus ejercicios.
-- RESTRICT en exercise: preserva el historial aunque el ejercicio
-- sea eliminado con soft delete.
-- ------------------------------------------------------------
CREATE TABLE session_exercises (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    session_id  BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    order_index INT    NOT NULL DEFAULT 0,
    notes       TEXT   NULL,
    CONSTRAINT pk_session_exercises PRIMARY KEY (id),
    CONSTRAINT fk_se_session        FOREIGN KEY (session_id)
                                    REFERENCES training_sessions(id)
                                    ON DELETE CASCADE,
    CONSTRAINT fk_se_exercise       FOREIGN KEY (exercise_id)
                                    REFERENCES exercises(id)
);

CREATE INDEX idx_se_session_exercise ON session_exercises(session_id, exercise_id);


-- ------------------------------------------------------------
-- 10. sets
-- Series realizadas dentro de un ejercicio de sesión.
-- Copiados desde routine_sets al ejecutar una rutina,
-- o agregados manualmente en sesiones libres.
-- weight: DECIMAL(5,2) soporta hasta 999.99 kg con exactitud garantizada.
-- CASCADE: al eliminar session_exercise se eliminan sus sets.
-- ------------------------------------------------------------
CREATE TABLE sets (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    session_exercise_id BIGINT       NOT NULL,
    set_number          INT          NOT NULL,
    weight              DECIMAL(5,2) NOT NULL,
    reps                INT          NOT NULL,
    notes               TEXT         NULL,
    CONSTRAINT pk_sets            PRIMARY KEY (id),
    CONSTRAINT fk_sets_session_ex FOREIGN KEY (session_exercise_id)
                                  REFERENCES session_exercises(id)
                                  ON DELETE CASCADE
);

CREATE INDEX idx_sets_session_exercise_id ON sets(session_exercise_id);


-- ------------------------------------------------------------
-- 11. gym_exercise_conversions
-- Feature opcional de normalización de pesos entre gimnasios.
-- Permite comparar el progreso histórico cuando el usuario
-- entrena en diferentes gyms con máquinas que tienen distintas
-- sensaciones de peso.
-- Semántica: peso_en_gym_id × conversion_factor = equivalente en reference_gym_id
-- conversion_factor DECIMAL(5,4): 4 decimales para evitar pérdida
-- de precisión al multiplicar antes de redondear a 2 decimales.
-- UNIQUE: un usuario no puede tener dos factores para el mismo
-- par gym + ejercicio.
-- CASCADE en todos los FK: al eliminar usuario, gym o ejercicio
-- se eliminan sus conversiones asociadas.
-- ------------------------------------------------------------
CREATE TABLE gym_exercise_conversions (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    exercise_id       BIGINT       NOT NULL,
    gym_id            BIGINT       NOT NULL,
    reference_gym_id  BIGINT       NOT NULL,
    conversion_factor DECIMAL(5,4) NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_gym_exercise_conv   PRIMARY KEY (id),
    CONSTRAINT fk_gec_user            FOREIGN KEY (user_id)
                                      REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_gec_exercise        FOREIGN KEY (exercise_id)
                                      REFERENCES exercises(id) ON DELETE CASCADE,
    CONSTRAINT fk_gec_gym             FOREIGN KEY (gym_id)
                                      REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gec_reference_gym   FOREIGN KEY (reference_gym_id)
                                      REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT uq_gec UNIQUE (user_id, exercise_id, gym_id, reference_gym_id)
);

CREATE INDEX idx_gec_user_exercise ON gym_exercise_conversions(user_id, exercise_id);

-- ============================================================
-- GainsTrack — Limpieza de tablas
-- Orden inverso de dependencias para evitar errores de FK
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS gym_exercise_conversions;
DROP TABLE IF EXISTS sets;
DROP TABLE IF EXISTS session_exercises;
DROP TABLE IF EXISTS training_sessions;
DROP TABLE IF EXISTS routine_sets;
DROP TABLE IF EXISTS routine_exercises;
DROP TABLE IF EXISTS routines;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS gyms;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS muscle_groups;

SET FOREIGN_KEY_CHECKS = 1;
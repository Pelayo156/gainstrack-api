# GainsTrack API

Backend de GainsTrack, una aplicación móvil para registro y seguimiento
de progreso en el gimnasio. Desarrollado con Java 21 y Spring Boot 4.0.5.

---

## Contexto del proyecto

GainsTrack sigue un modelo **routine-first**: las rutinas son plantillas
de entrenamiento que el usuario ejecuta, generando sesiones históricas
inmutables organizadas como carpetas.

### Flujo principal

1. El usuario crea **rutinas** con ejercicios y sets de referencia
   (peso y reps actuales)
2. Antes de entrenar, el usuario elige el **gimnasio** donde va a
   entrenar en ese momento
3. Al **ejecutar una rutina**, se crea una sesión que copia como punto
   de partida:
   - los ejercicios y sets de la **última sesión** del usuario para esa
     misma rutina en ese mismo gimnasio (gymId null cuenta como un
     gimnasio más — "sesión libre de gimnasio"), si existe, o
   - los ejercicios y sets de referencia de la **plantilla de la
     rutina**, si es la primera vez que la entrena en ese gimnasio
4. El usuario ajusta pesos y reps reales durante el entrenamiento
5. Al terminar, ingresa notas y la sesión queda **inmutable**
6. Las sesiones se organizan dentro de la carpeta de su rutina,
   ordenadas de más reciente a más antigua

### Conceptos clave

- **Rutina libre** (`is_free = TRUE`): creada automáticamente al registrar
  el usuario. Recibe sesiones sin rutina específica. No se puede eliminar.
- **Sesiones inmutables**: una vez completadas no se editan — son registro
  histórico real del entrenamiento.
- **Ejercicios globales**: `user_id = NULL`, visibles para todos los usuarios.
- **Ejercicios privados**: `user_id = valor`, creados por el usuario.
  Usan soft delete para preservar el historial de sesiones.
- **Gym opcional**: las sesiones pueden no tener gym asociado.

---

## Stack tecnológico

- **Java 21** — Virtual Threads (Project Loom)
- **Spring Boot 4.0.5**
- **MySQL 8.0+**
- **JdbcClient** — SQL puro, sin JPA/Hibernate
- **Spring Security + JWT** (jjwt 0.12.6)
- **Maven**

---

## Requisitos

- Java 21+
- MySQL 8.0+
- Maven 3.8+

---

## Configuración local

### 1. Crear bases de datos

```sql
CREATE DATABASE gainstrack_dev;
CREATE DATABASE gainstrack_test;
CREATE DATABASE gainstrack_prod;
```

### 2. Ejecutar scripts SQL

```bash
# DDL — crear tablas
mysql -u root -p gainstrack_dev < src/main/resources/db/ddl.sql

# Seed — datos de prueba
mysql -u root -p gainstrack_dev < src/main/resources/db/seed.sql
```

### 3. Configurar variable de entorno

```bash
export JWT_SECRET=tu_secreto_base64_aqui
```

### 4. Ejecutar

```bash
mvn spring-boot:run
```

El servicio levanta en `http://localhost:8080` con el perfil `dev` por defecto.

---

## Variables de entorno

| Variable | Descripción | Requerida |
|---|---|---|
| `JWT_SECRET` | Secreto Base64 para firmar JWT | Sí |
| `DB_URL` | URL de conexión MySQL | Solo en prod |
| `DB_USERNAME` | Usuario de base de datos | Solo en prod |
| `DB_PASSWORD` | Contraseña de base de datos | Solo en prod |
| `JWT_EXPIRATION` | Expiración del token en ms | No (default: 30 días) |

---

## Autenticación

Todos los endpoints excepto `/api/v1/auth/**` requieren JWT:

```
Authorization: Bearer <token>
```

El token se obtiene al hacer login o register. Duración: 30 días.

---

## Endpoints principales

### Auth
```
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Rutinas
```
GET    /api/v1/routines
GET    /api/v1/routines/{id}
POST   /api/v1/routines
PATCH  /api/v1/routines/{id}
DELETE /api/v1/routines/{id}
GET    /api/v1/routines/{id}/sessions
POST   /api/v1/routines/{id}/exercises
DELETE /api/v1/routines/{id}/exercises/{routineExerciseId}
PATCH  /api/v1/routines/{id}/exercises/{routineExerciseId}
POST   /api/v1/routines/{id}/exercises/{routineExerciseId}/sets
DELETE /api/v1/routines/{id}/exercises/{routineExerciseId}/sets/{setId}
PATCH  /api/v1/routines/{id}/exercises/{routineExerciseId}/sets/{setId}
```

### Sesiones
```
GET    /api/v1/sessions
GET    /api/v1/sessions/last?routineId={routineId}&gymId={gymId}
GET    /api/v1/sessions/{id}
POST   /api/v1/sessions
PATCH  /api/v1/sessions/{id}
DELETE /api/v1/sessions/{id}
POST   /api/v1/sessions/{id}/exercises
DELETE /api/v1/sessions/{id}/exercises/{sessionExerciseId}
PATCH  /api/v1/sessions/{id}/exercises/{sessionExerciseId}
POST   /api/v1/sessions/{id}/exercises/{sessionExerciseId}/sets
DELETE /api/v1/sessions/{id}/exercises/{sessionExerciseId}/sets/{setId}
PATCH  /api/v1/sessions/{id}/exercises/{sessionExerciseId}/sets/{setId}
```

### Ejercicios
```
GET    /api/v1/exercises
POST   /api/v1/exercises
PUT    /api/v1/exercises/{id}
DELETE /api/v1/exercises/{id}
```

### Gimnasios
```
GET    /api/v1/gyms
POST   /api/v1/gyms
DELETE /api/v1/gyms/{id}
```

### Grupos musculares
```
GET    /api/v1/muscle-groups
```

---

## Estructura del proyecto

```
src/main/java/com/molina/gainstrack/api
├── config/       — Seguridad, JWT, filtros
├── controller/   — Endpoints REST
├── service/      — Lógica de negocio
├── repository/   — Acceso a datos (SQL puro)
├── dto/          — Objetos de transferencia de datos
│   ├── auth/
│   ├── exercise/
│   ├── gym/
│   ├── routine/
│   ├── session/
│   └── shared/
├── exception/    — Excepciones personalizadas y handler global
├── model/        — Entidades del dominio
└── utils/        — Utilidades transversales

src/main/resources
├── db/
│   ├── ddl.sql
│   └── seed.sql
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

---

## Decisiones de diseño

- **Sin JPA** — SQL puro con JdbcClient para control total del rendimiento
- **Sesiones inmutables** — preservan el historial real de entrenamiento
- **Soft delete en exercises** — preserva integridad del historial de sesiones
- **COALESCE en PATCH** — edición parcial sin sobrescribir campos no enviados
- **Sesión creada desde el último entrenamiento por gimnasio** — al crear una
  sesión se busca la última sesión del usuario para la misma rutina y el mismo
  gimnasio (comparando `gym_id` con el operador NULL-safe `<=>` de MySQL, para
  que "sin gimnasio" también cuente como un grupo válido) y se copian sus
  ejercicios y sets reales; si no existe, se cae a la plantilla de la rutina
- **Virtual Threads** — mejor rendimiento en operaciones I/O concurrentes

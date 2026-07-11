# ── Etapa 1: compilar el proyecto ──────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar dependencias primero para aprovechar el caché de capas
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: imagen final liviana ──────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar solo el JAR generado desde la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
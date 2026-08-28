package com.molina.gainstrack.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@SpringBootApplication(exclude = {
		DataJdbcRepositoriesAutoConfiguration.class
})
public class Application {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	private final DataSource dataSource;

	public Application(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData meta = connection.getMetaData();
			LOG.info("Base de datos conectada — url: {}, driver: {}, versión: {}.{}",
					meta.getURL(),
					meta.getDriverName(),
					meta.getDatabaseMajorVersion(),
					meta.getDatabaseMinorVersion());
		} catch (SQLException e) {
			LOG.error("Error al verificar conexión a la base de datos — {}", e.getMessage());
		}
	}

}

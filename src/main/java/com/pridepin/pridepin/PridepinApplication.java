package com.pridepin.pridepin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the PridePin Spring Boot application.
 * Enables JPA auditing (created_at/updated_at) and async execution for non-blocking email sending.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class PridepinApplication {

	/**
	 * Starts the Spring application context and embedded server.
	 *
	 * @param args command-line arguments (unused)
	 */
	public static void main(String[] args) {
		SpringApplication.run(PridepinApplication.class, args);
	}

}

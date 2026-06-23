package com.pridepin.pridepin.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration: API metadata, servers, and JWT Bearer security scheme
 * so the Swagger UI can send the token in the Authorization header.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "PridePin API",
        version     = "1.0.0",
        description = "Safe space mapping REST API for the transgender community. " +
                      "Register or login to receive a JWT, then use it as a Bearer token " +
                      "to access protected endpoints.",
        contact     = @Contact(name = "PridePin", url = "https://github.com/your-username/pridepin"),
        license     = @License(name = "MIT")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development"),
        @Server(url = "https://your-deployed-app.com", description = "Production")
    }
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    in           = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}

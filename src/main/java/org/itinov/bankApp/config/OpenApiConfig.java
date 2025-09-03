package org.itinov.bankApp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * Configuration class for OpenAPI (Swagger) documentation.
 * It sets up JWT Bearer authentication for secured endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );
    }

    @Bean
    public OpenApiCustomizer securityCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
            pathItem.readOperations().forEach(op -> {
                if (!path.startsWith("/api/public")) {
                    op.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                }
            })
        );
    }
}

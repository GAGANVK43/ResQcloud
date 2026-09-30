package com.resqcloud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI resQCloudOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ResQCloud Emergency Response Platform API")
                        .description("Intelligent Emergency Response & Resource Coordination Platform RESTful APIs. " +
                                "Supports citizen emergency reporting, real-time prioritization scoring, intelligent resource matching, " +
                                "responder tracking, role-based dashboards, and audit logging.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ResQCloud Architecture Team")
                                .email("architecture@resqcloud.internal"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token to authenticate.")));
    }
}

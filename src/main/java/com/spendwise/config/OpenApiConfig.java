package com.spendwise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import com.spendwise.controller.AuthSessionController;
import com.spendwise.controller.GoogleAuthController;
import com.spendwise.controller.PasswordAuthController;
import com.spendwise.controller.PasswordResetController;
import com.spendwise.controller.TelegramAdminController;
import com.spendwise.controller.TelegramAuthorizationController;
import com.spendwise.controller.TelegramMemoryController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI spendwiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spendwise API")
                        .version("v1")
                        .description("REST API for Spendwise personal finance management, authentication, API keys, and Telegram integration.")
                        .contact(new Contact().name("Spendwise")))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token. Use the token returned by password login or Google sign-in."))
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Spendwise API key. The key is shown only when it is created."))
                        .addSecuritySchemes("TelegramServiceAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Service token")
                                .description("Trusted Telegram service token for internal integration endpoints."))
                        .addSecuritySchemes("TelegramUserId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Telegram-User-Id")
                                .description("Telegram user ID used with the Telegram service token on applicable endpoints.")));
    }

    @Bean
    OperationCustomizer spendwiseSecurityDocumentation() {
        return (operation, handlerMethod) -> {
            Class<?> controller = handlerMethod.getBeanType();
            if (controller == TelegramAuthorizationController.class || controller == TelegramMemoryController.class) {
                operation.addSecurityItem(new SecurityRequirement().addList("TelegramServiceAuth"));
            } else if (controller == AuthSessionController.class || controller == TelegramAdminController.class) {
                operation.addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            } else if (controller != GoogleAuthController.class && controller != PasswordAuthController.class
                    && controller != PasswordResetController.class) {
                // Business operations accept either a JWT or API key, and trusted Telegram service calls
                // require both the service bearer token and the Telegram user ID header.
                operation.addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
                operation.addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
                operation.addSecurityItem(new SecurityRequirement()
                        .addList("TelegramServiceAuth")
                        .addList("TelegramUserId"));
            }
            return operation;
        };
    }
}

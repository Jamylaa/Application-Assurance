package tn.vermeg.gestionproduit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GestionProduit API")
                .description("API de gestion des produits, packs, garanties et recommandations IA pour la plateforme d'assurance intelligente")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Vermeg")
                    .email("contact@vermeg.com"))
                .license(new License()
                    .name("Private")
                    .url("https://vermeg.com")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .name("BearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT Keycloak. Format: Bearer {token}")));
    }
}

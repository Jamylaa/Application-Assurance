package tn.vermeg.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vermeg Assurance - API Gateway")
                        .description("Gateway pour l'API de gestion des produits d'assurance")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Vermeg Assurance")
                                .email("contact@vermeg.tn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Environment"),
                        new Server().url("http://gateway:8080").description("Docker Environment")
                ));
    }

    @Bean
    public GroupedOpenApi gestionProduitApi() {
        return GroupedOpenApi.builder()
                .group("gestion-produit")
                .pathsToMatch("/api/produits/**", "/api/packs/**", "/api/garanties/**", "/api/recommendations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi gestionUserApi() {
        return GroupedOpenApi.builder()
                .group("gestion-user")
                .pathsToMatch("/api/users/**", "/api/auth/**")
                .build();
    }
}

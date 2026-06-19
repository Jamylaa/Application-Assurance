package tn.vermeg.gestionproduit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class SwaggerConfig {

    @Value("${server.port:9093}")
    private String serverPort;
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    @Bean
    public OpenAPI gestionProduitOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Development server");
        Contact contact = new Contact();
        contact.setEmail("benabdeljamyla@gmail.com");
        contact.setName("Vermeg Tunisia");
        contact.setUrl("https://www.vermeg.tn");
        License license = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");
        Info info = new Info()
                .title("Gestion Produit API")
                .version("1.0.0")
                .contact(contact)
                .description("""
                    API pour la gestion des produits d'assurance, packs et garanties avec moteur de recommandation IA  
                    ## Fonctionnalités principales
                    - Gestion des produits d'assurance (Santé, Vie, Auto, Habitation)
                    - Gestion des packs d'assurance avec associations de garanties
                    - Gestion des garanties individuelles
                    - Chatbot IA pour la création et recommandation de packs
                    - Système de mémoire conversationnelle (RAG)
                    - Moteur de recommandation basé sur l'IA
                    ## Authentification
                    L'API utilise Keycloak pour l'authentification et l'autorisation.
                    Incluez le token JWT dans le header Authorization: Bearer {token}
                    ## Version
                    1.0.0 - Version initiale avec toutes les fonctionnalités principales
                    """)
                .license(license);

        // Configuration de la sécurité JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Authentification JWT Keycloak. Incluez le token dans le format: Bearer {token}");
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme);
        return new OpenAPI()
                .info(info)
                .servers(List.of(server))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}

package tn.vermeg.gestionuser.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:9092}")
    private String serverPort;

    @Bean
    public OpenAPI gestionUserOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Development server");

        Contact contact = new Contact();
        contact.setEmail("contact@vermeg.tn");
        contact.setName("Vermeg Tunisia");

        License license = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Gestion User API")
                .version("1.0")
                .contact(contact)
                .description("API pour la gestion des utilisateurs, authentification et autorisation")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}

package tn.vermeg.gateway.config;

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

    @Value("${server.port:9091}")
    private String serverPort;

    @Bean
    public OpenAPI gatewayOpenAPI() {
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
                .title("API Gateway")
                .version("1.0")
                .contact(contact)
                .description("API Gateway pour la plateforme d'assurance Vermeg - Routing et Load Balancing")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}

package tn.vermeg.gestionproduit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Isolé de la classe {@code @SpringBootApplication} pour que les slices de test
 * (ex: {@code @WebMvcTest}) qui excluent l'auto-configuration MongoDB ne tentent
 * pas de résoudre les beans d'auditing/repositories Mongo.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "tn.vermeg.gestionproduit.repositories")
public class MongoConfig {
}

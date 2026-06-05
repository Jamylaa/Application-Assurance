package tn.vermeg.gestionuser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvironmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Value("${spring.data.mongodb.uri:}")
    private String mongodbUri;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${eureka.client.service-url.defaultZone:}")
    private String eurekaUrl;

    @PostConstruct
    public void validateEnvironment() {
        logger.info("Validating environment configuration...");

        // Validate MongoDB URI
        if (mongodbUri == null || mongodbUri.trim().isEmpty()) {
            throw new IllegalStateException("MONGODB_URI is required but not configured");
        }
        logger.info("MONGODB_URI is configured");

        // Validate Email Configuration (warning only)
        if (mailHost == null || mailHost.trim().isEmpty()) {
            logger.warn("SPRING_MAIL_HOST is not configured. Email features will be disabled.");
        } else {
            logger.info("SPRING_MAIL_HOST is configured");
            if (mailUsername == null || mailUsername.trim().isEmpty()) {
                logger.warn("SPRING_MAIL_USERNAME is not configured. Email features may not work properly.");
            }
        }

        // Validate Eureka URL
        if (eurekaUrl == null || eurekaUrl.trim().isEmpty()) {
            throw new IllegalStateException("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is required but not configured");
        }
        logger.info("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is configured");

        logger.info("Environment validation completed successfully");
    }
}

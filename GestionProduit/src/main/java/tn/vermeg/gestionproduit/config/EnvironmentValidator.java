package tn.vermeg.gestionproduit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvironmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${spring.data.mongodb.uri:}")
    private String mongodbUri;

    @Value("${eureka.client.service-url.defaultZone:}")
    private String eurekaUrl;

    @PostConstruct
    public void validateEnvironment() {
        logger.info("Validating environment configuration...");

        // Validate Gemini API Key (warning only, as AI features can be disabled)
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            logger.warn("GEMINI_API_KEY is not configured. AI features will be disabled.");
        } else {
            logger.info("GEMINI_API_KEY is configured");
        }

        // Validate MongoDB URI
        if (mongodbUri == null || mongodbUri.trim().isEmpty()) {
            throw new IllegalStateException("MONGODB_URI is required but not configured");
        }
        logger.info("MONGODB_URI is configured");

        // Validate Eureka URL
        if (eurekaUrl == null || eurekaUrl.trim().isEmpty()) {
            throw new IllegalStateException("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is required but not configured");
        }
        logger.info("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is configured");

        logger.info("Environment validation completed successfully");
    }
}

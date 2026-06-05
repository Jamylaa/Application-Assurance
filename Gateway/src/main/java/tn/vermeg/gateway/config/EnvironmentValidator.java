package tn.vermeg.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvironmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String jwtIssuerUri;

    @Value("${eureka.client.service-url.defaultZone:}")
    private String eurekaUrl;

    @PostConstruct
    public void validateEnvironment() {
        logger.info("Validating Gateway environment configuration...");

        // Validate JWT Issuer URI
        if (jwtIssuerUri == null || jwtIssuerUri.trim().isEmpty()) {
            throw new IllegalStateException("JWT_ISSUER_URI is required but not configured");
        }
        logger.info("JWT_ISSUER_URI is configured: {}", jwtIssuerUri);

        // Validate Eureka URL
        if (eurekaUrl == null || eurekaUrl.trim().isEmpty()) {
            throw new IllegalStateException("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is required but not configured");
        }
        logger.info("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is configured");

        logger.info("Gateway environment validation completed successfully");
    }
}

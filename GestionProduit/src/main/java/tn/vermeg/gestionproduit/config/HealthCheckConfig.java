package tn.vermeg.gestionproduit.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.netflix.discovery.EurekaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class HealthCheckConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckConfig.class);

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public HealthIndicator mongoHealthIndicator(MongoClient mongoClient) {
        return () -> {
            long startTime = System.currentTimeMillis();
            try {
                MongoDatabase database = mongoClient.getDatabase(databaseName);
                database.runCommand(org.bson.BsonDocument.parse("{ping:1}"));
                long duration = System.currentTimeMillis() - startTime;
                logger.info("MongoDB health check passed in {} ms", duration);
                return Health.up()
                        .withDetail("database", databaseName)
                        .withDetail("status", "Connected")
                        .withDetail("responseTime", duration + "ms")
                        .build();
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("MongoDB health check failed after {} ms: {}", duration, e.getMessage());
                return Health.down()
                        .withDetail("database", databaseName)
                        .withDetail("error", e.getMessage())
                        .withDetail("responseTime", duration + "ms")
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator eurekaHealthIndicator(DiscoveryClient discoveryClient) {
        return () -> {
            long startTime = System.currentTimeMillis();
            try {
                boolean registered = discoveryClient.getServices().size() > 0;
                long duration = System.currentTimeMillis() - startTime;
                
                if (registered) {
                    logger.info("Eureka health check passed in {} ms", duration);
                    return Health.up()
                            .withDetail("service", "Eureka Client")
                            .withDetail("status", "Registered")
                            .withDetail("services", discoveryClient.getServices())
                            .withDetail("responseTime", duration + "ms")
                            .build();
                } else {
                    logger.warn("Eureka health check: not registered");
                    return Health.down()
                            .withDetail("service", "Eureka Client")
                            .withDetail("status", "Not Registered")
                            .withDetail("responseTime", duration + "ms")
                            .build();
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Eureka health check failed after {} ms: {}", duration, e.getMessage());
                return Health.down()
                        .withDetail("service", "Eureka Client")
                        .withDetail("error", e.getMessage())
                        .withDetail("responseTime", duration + "ms")
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator geminiHealthIndicator() {
        return () -> {
            try {
                String apiKey = System.getenv("GEMINI_API_KEY");
                boolean enabled = Boolean.parseBoolean(System.getenv().getOrDefault("GEMINI_ENABLED", "true"));
                
                if (!enabled) {
                    return Health.up()
                            .withDetail("service", "Gemini AI")
                            .withDetail("status", "Disabled")
                            .build();
                }
                
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    return Health.down()
                            .withDetail("service", "Gemini AI")
                            .withDetail("status", "No API Key")
                            .build();
                }
                
                return Health.up()
                        .withDetail("service", "Gemini AI")
                        .withDetail("status", "Configured")
                        .withDetail("apiKey", "***" + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : ""))
                        .build();
            } catch (Exception e) {
                logger.error("Gemini health check failed: {}", e.getMessage());
                return Health.down()
                        .withDetail("service", "Gemini AI")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}

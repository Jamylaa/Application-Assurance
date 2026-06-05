package tn.vermeg.gestionuser.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class HealthCheckConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckConfig.class);

    @Value("${spring.data.mongodb.database:vermeg_db}")
    private String databaseName;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private String mailPort;

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
    public HealthIndicator emailHealthIndicator() {
        return () -> {
            try {
                String mailUsername = System.getenv("MAIL_USERNAME");
                String mailPassword = System.getenv("MAIL_PASSWORD");
                
                if (mailHost == null || mailHost.trim().isEmpty()) {
                    return Health.down()
                            .withDetail("service", "Email Service")
                            .withDetail("status", "Not Configured")
                            .withDetail("error", "Mail host not configured")
                            .build();
                }
                
                if (mailUsername == null || mailUsername.trim().isEmpty()) {
                    return Health.down()
                            .withDetail("service", "Email Service")
                            .withDetail("status", "Not Configured")
                            .withDetail("error", "Mail username not configured")
                            .build();
                }
                
                logger.info("Email service health check passed");
                return Health.up()
                        .withDetail("service", "Email Service")
                        .withDetail("status", "Configured")
                        .withDetail("host", mailHost)
                        .withDetail("port", mailPort)
                        .withDetail("username", mailUsername)
                        .build();
            } catch (Exception e) {
                logger.error("Email health check failed: {}", e.getMessage());
                return Health.down()
                        .withDetail("service", "Email Service")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}

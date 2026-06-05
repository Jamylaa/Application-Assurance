package tn.vermeg.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class CircuitBreakerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testFallbackUserEndpoint() throws Exception {
        // Simulate a scenario where the user service is down
        webTestClient.get()
                .uri("/fallback/user")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.service").isEqualTo("GESTIONUSER");
    }

    @Test
    public void testFallbackProductEndpoint() throws Exception {
        webTestClient.get()
                .uri("/fallback/product")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.service").isEqualTo("GESTIONPRODUIT");
    }

    @Test
    public void testFallbackRecommendationEndpoint() throws Exception {
        webTestClient.get()
                .uri("/fallback/recommendation")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.service").isEqualTo("GESTIONPRODUIT");
    }

    @Test
    public void testGatewayHealthEndpoint() throws Exception {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }
}

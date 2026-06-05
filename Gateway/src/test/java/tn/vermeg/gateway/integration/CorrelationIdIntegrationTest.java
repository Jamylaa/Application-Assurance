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
public class CorrelationIdIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testCorrelationIdGenerated() throws Exception {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-ID");
    }

    @Test
    public void testCorrelationIdPropagated() throws Exception {
        String correlationId = "test-correlation-id-12345";

        webTestClient.get()
                .uri("/actuator/health")
                .header("X-Correlation-ID", correlationId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-ID", correlationId);
    }
}

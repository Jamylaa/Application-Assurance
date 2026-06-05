package tn.vermeg.gestionproduit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HealthCheckIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    public void testMongoDBHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health/mongo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.details.database").exists());
    }

    @Test
    public void testEurekaHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health/eureka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.details.service").exists());
    }

    @Test
    public void testGeminiHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health/gemini"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.details.service").exists());
    }
}

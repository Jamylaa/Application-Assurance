package tn.vermeg.gestionproduit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ChatbotIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCreateGarantieViaChatbot() throws Exception {
        String prompt = "Créer une garantie hospitalisation avec un taux de remboursement de 80% et un plafond annuel de 50000€";

        mockMvc.perform(post("/api/chatbot/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\": \"" + prompt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.action").value("GARANTIE"))
                .andExpect(jsonPath("$.result.message").exists())
                .andExpect(jsonPath("$.result.id").exists());
    }

    @Test
    public void testCreateProduitViaChatbot() throws Exception {
        String prompt = "Créer un produit d'assurance santé avec une description complète";

        mockMvc.perform(post("/api/chatbot/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\": \"" + prompt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.action").value("PRODUIT"))
                .andExpect(jsonPath("$.result.message").exists())
                .andExpect(jsonPath("$.result.id").exists());
    }

    @Test
    public void testCreatePackViaChatbot() throws Exception {
        String prompt = "Créer un pack santé avec un prix mensuel de 50€";

        mockMvc.perform(post("/api/chatbot/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\": \"" + prompt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.action").value("PACK"))
                .andExpect(jsonPath("$.result.message").exists())
                .andExpect(jsonPath("$.result.id").exists());
    }

    @Test
    public void testGetRecommendationViaChatbot() throws Exception {
        String prompt = "Je cherche une assurance santé pour une personne de 30 ans avec un budget de 100€ par mois";

        mockMvc.perform(post("/api/chatbot/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\": \"" + prompt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.action").value("RECOMMENDATION"))
                .andExpect(jsonPath("$.result.topPacks").exists())
                .andExpect(jsonPath("$.result.topProduits").exists())
                .andExpect(jsonPath("$.result.criteria").exists());
    }

    @Test
    public void testInvalidPrompt() throws Exception {
        String prompt = "";

        mockMvc.perform(post("/api/chatbot/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\": \"" + prompt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }
}

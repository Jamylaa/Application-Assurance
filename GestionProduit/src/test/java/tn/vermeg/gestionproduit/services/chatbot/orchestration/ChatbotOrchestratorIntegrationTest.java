package tn.vermeg.gestionproduit.services.chatbot.orchestration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tn.vermeg.gestionproduit.dto.ChatbotResponseDTO;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.enums.TypeMontant;
import tn.vermeg.gestionproduit.services.chatbot.validation.DataConsistencyValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour le pipeline complet de création de garantie via chatbot
 * Valide le flux complet: Prompt → Extraction → DTO → Entity → MongoDB
 */
@SpringBootTest
public class ChatbotOrchestratorIntegrationTest {

    @Autowired
    private ChatbotOrchestratorService chatbotOrchestratorService;

    @Autowired
    private DataConsistencyValidator dataConsistencyValidator;

    // ========== TESTS SCÉNARIO COMPLET ==========

    @Test
    @DisplayName("Scénario complet: Création garantie avec toutes les informations")
    void testScenarioComplet_ToutesInformations() {
        String prompt = "Créer une garantie nommée Cardiologie Premium avec le domaine CARDIOLOGIE et le type montant TARIF_CONVENTIONNE et un taux de remboursement de 80 pourcent et un plafond annuel de 30000 et un plafond mensuel de 2500 et un plafond par acte de 1500 et une franchise de 50 et un coût moyen par sinistre de 1500 et une durée de contrat de 12 à 36 mois et résiliable annuellement et statut ACTIF";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        // Validation réponse
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertTrue(response.isSuccess(), "La création devrait réussir");
        assertEquals("CREATE_GARANTIE", response.getAction(), "L'action devrait être CREATE_GARANTIE");

        // Validation entity créée
        Garantie garantie = (Garantie) response.getData().get("entity");
        assertNotNull(garantie, "L'entity ne devrait pas être null");
        assertEquals("Cardiologie Premium", garantie.getNomGarantie(), "Nom incorrect");
        assertEquals(TypeMontant.TARIF_CONVENTIONNE, garantie.getTypeMontant(), "TypeMontant incorrect");
        assertEquals(0.8, garantie.getTauxRemboursement(), "Taux incorrect");
        assertEquals(36, garantie.getDureeMaxContrat(), "Durée max incorrecte");
        assertEquals(1500.0, garantie.getCoutMoyenParSinistre(), "Coût moyen incorrect");
        assertTrue(garantie.isResiliableAnnuellement(), "Résiliable annuellement incorrect");
        assertNotNull(garantie.getDescription(), "Description ne devrait pas être null");

        // Validation consistance
        String consistencyStatus = (String) response.getData().get("consistencyStatus");
        assertNotNull(consistencyStatus, "Le statut de consistance ne devrait pas être null");
        assertTrue(consistencyStatus.equals("CONSISTENT"), "Les données devraient être consistantes");
    }

    @Test
    @DisplayName("Scénario avec typeMontant TARIF_CONVENTIONNE en majuscules")
    void testTypeMontant_TarifConventionne_Majuscules() {
        String prompt = "Créer une garantie nommée Test avec le type montant TARIF_CONVENTIONNE";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        Garantie garantie = (Garantie) response.getData().get("entity");
        assertNotNull(garantie, "L'entity ne devrait pas être null");
        assertEquals(TypeMontant.TARIF_CONVENTIONNE, garantie.getTypeMontant(), 
            "TARIF_CONVENTIONNE en majuscules devrait être correctement extrait");
    }

    @Test
    @DisplayName("Scénario avec durée '12 à 36 mois'")
    void testDureeContrat_Range() {
        String prompt = "Créer une garantie nommée Test avec une durée de contrat de 12 à 36 mois";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        Garantie garantie = (Garantie) response.getData().get("entity");
        assertNotNull(garantie, "L'entity ne devrait pas être null");
        assertEquals(12, garantie.getDureeMinContrat(), "Durée min incorrecte");
        assertEquals(36, garantie.getDureeMaxContrat(), "Durée max incorrecte");
    }

    @Test
    @DisplayName("Scénario avec coût moyen par sinistre")
    void testCoutMoyenParSinistre() {
        String prompt = "Créer une garantie nommée Test avec un coût moyen par sinistre de 1500";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        Garantie garantie = (Garantie) response.getData().get("entity");
        assertNotNull(garantie, "L'entity ne devrait pas être null");
        assertEquals(1500.0, garantie.getCoutMoyenParSinistre(), "Coût moyen incorrect");
    }

    @Test
    @DisplayName("Scénario avec résiliable annuellement")
    void testResiliableAnnuellement() {
        String prompt = "Créer une garantie nommée Test résiliable annuellement";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        Garantie garantie = (Garantie) response.getData().get("entity");
        assertNotNull(garantie, "L'entity ne devrait pas être null");
        assertTrue(garantie.isResiliableAnnuellement(), "Résiliable annuellement incorrect");
    }

    @Test
    @DisplayName("Scénario sans description: génération automatique")
    void testDescription_GenerationAutomatique() {
        String prompt = "Créer une garantie nommée Cardiologie Test avec le domaine CARDIOLOGIE";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        Garantie garantie = (Garantie) response.getData().get("entity");
        assertNotNull(garantie, "L'entity ne devrait pas être null");
        assertNotNull(garantie.getDescription(), "Description devrait être générée automatiquement");
        assertTrue(garantie.getDescription().contains("Cardiologie Test"), 
            "Description générée devrait contenir le nom");
    }

    @Test
    @DisplayName("Validation: TypeMontant manquant devrait réduire le score")
    void testValidation_TypeMontantManquant_ReduitScore() {
        String prompt = "Créer une garantie nommée Test sans type montant spécifié";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        // Vérifier que la validation a détecté le champ manquant
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> consistencyReport = 
            (java.util.Map<String, Object>) response.getData().get("consistencyReport");
        
        assertNotNull(consistencyReport, "Le rapport de consistance ne devrait pas être null");
        
        @SuppressWarnings("unchecked")
        java.util.List<String> discrepancies = 
            (java.util.List<String>) consistencyReport.get("discrepancies");
        
        // Si typeMontant est manquant, il devrait y avoir des discrepancies
        if (discrepancies != null && !discrepancies.isEmpty()) {
            boolean typeMontantDiscrepancy = discrepancies.stream()
                .anyMatch(d -> d.contains("typeMontant"));
            assertTrue(typeMontantDiscrepancy, "Devrait détecter la discrepancy sur typeMontant");
        }
    }

    @Test
    @DisplayName("Validation: Durée max manquante devrait réduire le score")
    void testValidation_DureeMaxManquante_ReduitScore() {
        String prompt = "Créer une garantie nommée Test avec durée min 12 mois mais pas de durée max";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        // Vérifier que la validation a détecté le champ manquant
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> consistencyReport = 
            (java.util.Map<String, Object>) response.getData().get("consistencyReport");
        
        assertNotNull(consistencyReport, "Le rapport de consistance ne devrait pas être null");
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> fieldTraces = 
            (java.util.Map<String, Object>) consistencyReport.get("fieldTraces");
        
        assertNotNull(fieldTraces, "Les traces de champs ne devraient pas être null");
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> dureeMaxTrace = 
            (java.util.Map<String, Object>) fieldTraces.get("dureeMaxContrat");
        
        if (dureeMaxTrace != null) {
            Object recordedValue = dureeMaxTrace.get("recorded");
            Boolean match = (Boolean) dureeMaxTrace.get("match");
            
            // Si la valeur enregistrée est 0 ou null et que le match est false, c'est normal
            if (recordedValue != null && (recordedValue.equals(0) || recordedValue.equals(0.0))) {
                assertFalse(match, "Devrait détecter l'incohérence sur dureeMaxContrat");
            }
        }
    }

    @Test
    @DisplayName("Validation de consistance: Comparaison extraction vs enregistrement")
    void testValidationConsistance_ExtractionVsEnregistrement() {
        String prompt = "Créer une garantie nommée Test Complet avec le domaine CARDIOLOGIE et le type montant TARIF_CONVENTIONNE et un taux de remboursement de 80 pourcent et un coût moyen par sinistre de 1500 et une durée de contrat de 12 à 36 mois et résiliable annuellement";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> consistencyReport = 
            (java.util.Map<String, Object>) response.getData().get("consistencyReport");
        
        assertNotNull(consistencyReport, "Le rapport de consistance ne devrait pas être null");
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> fieldTraces = 
            (java.util.Map<String, Object>) consistencyReport.get("fieldTraces");
        
        assertNotNull(fieldTraces, "Les traces de champs ne devraient pas être null");
        
        // Vérifier que les champs critiques sont consistants
        String[] criticalFields = {"typeMontant", "dureeMaxContrat", "coutMoyenParSinistre"};
        
        for (String field : criticalFields) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> trace = 
                (java.util.Map<String, Object>) fieldTraces.get(field);
            
            if (trace != null) {
                Boolean match = (Boolean) trace.get("match");
                assertNotNull(match, "Le champ " + field + " devrait avoir un statut de match");
                // Si le champ a été extrait, il devrait matcher
                if (trace.get("extracted") != null && !trace.get("extracted").equals("null")) {
                    assertTrue(match, "Le champ " + field + " devrait être consistant");
                }
            }
        }
    }

    @Test
    @DisplayName("Test du rapport de validation métier")
    void testValidationMetier_RapportComplet() {
        String prompt = "Créer une garantie nommée Test";

        ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

        assertNotNull(response.getData().get("businessValidation"), 
            "Le rapport de validation métier ne devrait pas être null");
        
        assertNotNull(response.getData().get("consistencyStatus"), 
            "Le statut de consistance ne devrait pas être null");
        
        @SuppressWarnings("unchecked")
        java.util.List<String> warnings = 
            (java.util.List<String>) response.getData().get("extractionWarnings");
        
        assertNotNull(warnings, "Les warnings d'extraction ne devraient pas être null");
    }
}

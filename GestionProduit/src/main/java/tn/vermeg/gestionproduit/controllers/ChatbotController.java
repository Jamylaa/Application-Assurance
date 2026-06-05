package tn.vermeg.gestionproduit.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.dto.ChatbotPromptRequestDTO;
import tn.vermeg.gestionproduit.dto.ChatbotResponseDTO;
import tn.vermeg.gestionproduit.services.chatbot.ChatbotOrchestratorService;

import jakarta.validation.Valid;
import java.util.Map;
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);

    private final ChatbotOrchestratorService chatbotOrchestratorService;

    public ChatbotController(ChatbotOrchestratorService chatbotOrchestratorService) {
        this.chatbotOrchestratorService = chatbotOrchestratorService;
    }
    @PostMapping("/process")
    public ResponseEntity<ChatbotResponseDTO> processPrompt(@Valid @RequestBody ChatbotPromptRequestDTO request) {
        try {
            String prompt = request.getPrompt();

            logger.info("Traitement du prompt: {}", prompt);

            ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt);

            if (response.isSuccess()) {
                logger.info("Prompt traité avec succès: {}", response.getAction());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Échec du traitement du prompt: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du prompt", e);
            
            ChatbotResponseDTO errorResponse = new ChatbotResponseDTO();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Erreur interne du serveur");
            errorResponse.addError("Une erreur est survenue lors du traitement: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

   //Endpoint de vérification de l'état du chatbot(le service est fonctionnel)
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
            "status", "UP",
            "service", "Chatbot Gestion Service",
            "description", "Chatbot intelligent pour la création et configuration des garanties, produits et packs",
            "version", "1.0.0",
            "architecture", "Unifiée et Centralisée",
            "timestamp", System.currentTimeMillis(),
            "endpoints", Map.of(
                "process", "/api/chatbot/process",
                "health", "/api/chatbot/health",
                "test", "/api/chatbot/test"
            ),
            "supportedActions", Map.of(
                "GARANTIE", "Création de garanties",
                "PRODUIT", "Création de produits", 
                "PACK", "Création de packs",
                "CONFIGURATION_PACK", "Configuration de packs",
                "AJOUT_GARANTIE_PACK", "Ajout de garanties aux packs",
                "RECOMMENDATION", "Recommandation personnalisée"
            )
        );

        return ResponseEntity.ok(response);
    }

   //Endpoint de test pour vérifier que le contrôleur fonctionne
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = Map.of(
            "status", "OK",
            "message", "Chatbot Controller is working!",
            "service", "GestionProduit Chatbot Service",
            "timestamp", System.currentTimeMillis(),
            "version", "1.0.0",
            "architecture", "PFE Ingénieur - Niveau Professionnel"
        );

        return ResponseEntity.ok(response);
    }

    //Endpoint pour obtenir des informations sur les actions supportées
    @GetMapping("/actions")
    public ResponseEntity<Map<String, Object>> getSupportedActions() {
        Map<String, Object> response = Map.of(
            "supportedActions", Map.of(
                "GARANTIE", Map.of(
                    "description", "Création d'une garantie",
                    "example", "Créer une garantie hospitalisation premium avec un remboursement de 90%"
                ),
                "PRODUIT", Map.of(
                    "description", "Création d'un produit",
                    "example", "Créer un produit d'assurance santé nommé 'Santé Premium'"
                ),
                "PACK", Map.of(
                    "description", "Création d'un pack",
                    "example", "Créer un pack Gold lié au produit Santé Premium"
                ),
                "CONFIGURATION_PACK", Map.of(
                    "description", "Configuration d'un pack existant",
                    "example", "Configurer le pack Silver avec les garanties optique et dentaire"
                ),
                "AJOUT_GARANTIE_PACK", Map.of(
                    "description", "Ajout d'une garantie à un pack",
                    "example", "Ajouter la garantie optique au pack Silver"
                ),
                "RECOMMENDATION", Map.of(
                    "description", "Recommandation personnalisée de produits, packs et garanties",
                    "example", "Recommande moi un produit santé avec garantie hospitalisation"
                )
            ),
            "features", Map.of(
                "aiExtraction", "Extraction intelligente des données via Google AI",
                "normalization", "Normalisation automatique des enums et valeurs",
                "validation", "Validation robuste des données extraites",
                "businessLogicReuse", "Réutilisation des services métier existants",
                "errorHandling", "Gestion centralisée des erreurs",
                "logging", "Logs structurés avec SLF4J"
            ),
            "architecture", Map.of(
                "controller", "ChatbotController (ce controller)",
                "orchestrator", "ChatbotOrchestratorService",
                "services", "PromptAnalyzerService, AIExtractionService, ActionNormalizationService, ValidationService",
                "businessServices", "GarantieService, ProduitService, PackUnifiedService",
                "separation", "Claire séparation entre logique chatbot et logique métier"
            )
        );

        return ResponseEntity.ok(response);
    }
// Endpoint pour vérifier la disponibilité du service IA
       @GetMapping("/ai-status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        // Cette vérification sera implémentée dans l'orchestrator
        boolean aiAvailable = true; // Placeholder
        
        Map<String, Object> response = Map.of(
            "aiAvailable", aiAvailable,
            "provider", "Google AI (Gemini)",
            "fallbackMode", !aiAvailable,
            "message", aiAvailable ? 
                "Service IA disponible - Extraction intelligente activée" : 
                "Service IA indisponible - Mode fallback activé (extraction par patterns)"
        );

        return ResponseEntity.ok(response);
    }
}
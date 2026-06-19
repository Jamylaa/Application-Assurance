package tn.vermeg.gestionproduit.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.dto.ChatbotResponseDTO;
import tn.vermeg.gestionproduit.dto.ChatbotRequestDTO;
import tn.vermeg.gestionproduit.services.chatbot.orchestration.ChatbotOrchestratorService;

import jakarta.validation.Valid;

import java.util.HashMap;
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
    public ResponseEntity<ChatbotResponseDTO> processPrompt(@Valid @RequestBody ChatbotRequestDTO request) {
        try {
            String prompt = request.getPrompt();
            String sessionId = request.getSessionId();

            logger.info("Traitement du prompt: {} (sessionId: {})", prompt, sessionId);

            ChatbotResponseDTO response = chatbotOrchestratorService.processPrompt(prompt, sessionId);

            logger.info("Réponse du chatbot - Action: {}, Success: {}, Message: {}, SessionId: {}", 
                response.getAction(), response.isSuccess(), response.getMessage(), response.getData().get("sessionId"));
            
            if (response.getResult() != null) {
                logger.info("Réponse du chatbot - Result class: {}", response.getResult().getClass().getName());
            }

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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Chatbot Gestion Service");
        response.put("description", "Chatbot intelligent pour la création et configuration des garanties, produits et packs");
        response.put("version", "1.0.0");
        response.put("architecture", "Unifiée et Centralisée");
        response.put("timestamp", System.currentTimeMillis());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("process", "/api/chatbot/process");
        endpoints.put("health", "/api/chatbot/health");
        endpoints.put("test", "/api/chatbot/test");
        response.put("endpoints", endpoints);
        
        Map<String, String> supportedActions = new HashMap<>();
        supportedActions.put("GARANTIE", "Création de garanties");
        supportedActions.put("PRODUIT", "Création de produits");
        supportedActions.put("PACK", "Création de packs");
        supportedActions.put("CONFIGURATION_PACK", "Configuration de packs");
        supportedActions.put("AJOUT_GARANTIE_PACK", "Ajout de garanties aux packs");
        response.put("supportedActions", supportedActions);

        return ResponseEntity.ok(response);
    }

   //Endpoint de test pour vérifier que le contrôleur fonctionne
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Chatbot Controller is working!");
        response.put("service", "GestionProduit Chatbot Service");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        response.put("architecture", "PFE Ingénieur - Niveau Professionnel");

        return ResponseEntity.ok(response);
    }

    //Endpoint pour obtenir des informations sur les actions supportées
    @GetMapping("/actions")
    public ResponseEntity<Map<String, Object>> getSupportedActions() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> supportedActions = new HashMap<>();
        
        Map<String, String> garantie = new HashMap<>();
        garantie.put("description", "Création d'une garantie");
        garantie.put("example", "Créer une garantie hospitalisation premium avec un remboursement de 90%");
        supportedActions.put("GARANTIE", garantie);
        
        Map<String, String> produit = new HashMap<>();
        produit.put("description", "Création d'un produit");
        produit.put("example", "Créer un produit d'assurance santé nommé 'Santé Premium'");
        supportedActions.put("PRODUIT", produit);
        
        Map<String, String> pack = new HashMap<>();
        pack.put("description", "Création d'un pack");
        pack.put("example", "Créer un pack Gold lié au produit Santé Premium");
        supportedActions.put("PACK", pack);
        
        Map<String, String> configPack = new HashMap<>();
        configPack.put("description", "Configuration d'un pack existant");
        configPack.put("example", "Configurer le pack Silver avec les garanties optique et dentaire");
        supportedActions.put("CONFIGURATION_PACK", configPack);
        
        Map<String, String> ajoutGarantie = new HashMap<>();
        ajoutGarantie.put("description", "Ajout d'une garantie à un pack");
        ajoutGarantie.put("example", "Ajouter la garantie optique au pack Silver");
        supportedActions.put("AJOUT_GARANTIE_PACK", ajoutGarantie);
        
        Map<String, String> recommandation = new HashMap<>();
        recommandation.put("description", "Recommandation intelligente de packs et produits basée sur le profil client");
        recommandation.put("example", "Je suis un homme de 45 ans, marié, avec deux enfants. Je recherche un pack adapté à ma famille avec couverture dentaire et kinésithérapie");
        supportedActions.put("RECOMMANDATION", recommandation);
        
        response.put("supportedActions", supportedActions);
        
        Map<String, String> features = new HashMap<>();
        features.put("aiExtraction", "Extraction intelligente des données via Google AI");
        features.put("normalization", "Normalisation automatique des enums et valeurs");
        features.put("validation", "Validation robuste des données extraites");
        features.put("businessLogicReuse", "Réutilisation des services métier existants");
        features.put("errorHandling", "Gestion centralisée des erreurs");
        features.put("logging", "Logs structurés avec SLF4J");
        response.put("features", features);
        
        Map<String, String> architecture = new HashMap<>();
        architecture.put("controller", "ChatbotController (ce controller)");
        architecture.put("orchestrator", "ChatbotOrchestratorService");
        architecture.put("services", "PromptAnalyzerService, AIExtractionService, ActionNormalizationService, ValidationService");
        architecture.put("businessServices", "GarantieService, ProduitService, PackUnifiedService");
        architecture.put("separation", "Claire séparation entre logique chatbot et logique métier");
        response.put("architecture", architecture);

        return ResponseEntity.ok(response);
    }
// Endpoint pour vérifier la disponibilité du service IA
       @GetMapping("/ai-status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        // Cette vérification sera implémentée dans l'orchestrator
        boolean aiAvailable = true; // Placeholder
        
        Map<String, Object> response = new HashMap<>();
        response.put("aiAvailable", aiAvailable);
        response.put("provider", "Google AI (Gemini)");
        response.put("fallbackMode", !aiAvailable);
        response.put("message", aiAvailable ? 
                "Service IA disponible - Extraction intelligente activée" : 
                "Service IA indisponible - Mode fallback activé (extraction par patterns)");

        return ResponseEntity.ok(response);
    }
}

// Endpoint principal pour le traitement des prompts
// Gère la création et la configuration des garanties, produits et packs
//- {"prompt": "Créer une garantie hospitalisation premium active avec un remboursement de 90% sur les frais réels, un plafond annuel de 50000, un plafond mensuel de 10000 et un plafond par acte de 5000, avec une franchise de 100, un coût moyen par sinistre de 2000, une durée de contrat comprise entre 12 et 60 mois, résiliable annuellement"}
// - {"prompt": "Créer un produit d'assurance santé nommé 'Produit Assurance Santé Minimum', avec la description 'Couverture médicale complète pour particuliers et familles', de type SANTE et avec le statut ACTIF"}
// - {"prompt": "Créer un pack Gold lié au produit Santé Premium avec les garanties hospitalisation et dentaire"}
// - {"prompt": "Ajouter la garantie optique au pack Silver"}
// - {"prompt": "Créer un pack santé premium nommé 'Santé Premium Gold' pour un produit SANTE intitulé 'Produit Assurance Santé Minimum'. Le pack doit respecter les conditions suivantes : un âge minimum de 18 ans et un âge maximum de 70 ans, un type de clients FAMILLE et INDIVIDUEL, une ancienneté minimale de 0 mois, une couverture géographique INTERNATIONAL, un prix mensuel de 120, une durée de contrat comprise entre 12 et 60 mois, un niveau de couverture GOLD et un statut ACTIF"}

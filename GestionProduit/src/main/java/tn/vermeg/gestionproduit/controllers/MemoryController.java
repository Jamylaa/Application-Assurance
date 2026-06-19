package tn.vermeg.gestionproduit.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.entities.ConversationMemory;
import tn.vermeg.gestionproduit.services.chatbot.memory.ConversationMemoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private static final Logger logger = LoggerFactory.getLogger(MemoryController.class);
    private final ConversationMemoryService memoryService;
    public MemoryController(ConversationMemoryService memoryService) {
        this.memoryService = memoryService;
    }
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionMemory(@PathVariable String sessionId) {
        try {
            Map<String, Object> context = memoryService.getConversationContext(sessionId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionId", sessionId,
                "context", context
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la mémoire: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> getMessageHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ConversationMemory.MessageExchange> messages = memoryService.getMessageHistory(sessionId, limit);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("messages", messages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    @PostMapping("/session/{sessionId}/feedback")
    public ResponseEntity<Map<String, Object>> recordFeedback(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> feedback) {
        try {
            String feedbackValue = feedback.get("feedback");
            memoryService.recordUserFeedback(sessionId, feedbackValue);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Feedback enregistré avec succès"
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de l'enregistrement du feedback: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    @PostMapping("/session/{sessionId}/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> preferences) {
        try {
            memoryService.updateUserPreferences(sessionId, preferences);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Préférences mises à jour avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour des préférences: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        try {
            memoryService.deleteConversationMemory(sessionId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session supprimée avec succès"
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la session: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldMemories() {
        try {
            memoryService.cleanupOldMemories();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nettoyage des anciennes mémoires effectué"
            ));
        } catch (Exception e) {
            logger.error("Erreur lors du nettoyage: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    @GetMapping("/session/new")
    public ResponseEntity<Map<String, Object>> newSession() {
        String sessionId = memoryService.generateSessionId();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "sessionId", sessionId
        ));
    }
}
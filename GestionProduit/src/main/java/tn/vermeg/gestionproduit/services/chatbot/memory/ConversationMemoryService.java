package tn.vermeg.gestionproduit.services.chatbot.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.RecommendationRequestDTO;
import tn.vermeg.gestionproduit.dto.RecommendationResponseDTO;
import tn.vermeg.gestionproduit.dto.RecommendationResultDTO;
import tn.vermeg.gestionproduit.entities.ConversationMemory;
import tn.vermeg.gestionproduit.repositories.ConversationMemoryRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationMemoryService.class);
    private static final int MAX_HISTORY_SIZE = 50;
    private static final int MAX_RECOMMENDATIONS_HISTORY = 20;

    private final ConversationMemoryRepository memoryRepository;

    public ConversationMemoryService(ConversationMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    /**
     * Récupère ou crée une mémoire de conversation pour un utilisateur
     */
    public ConversationMemory getOrCreateConversationMemory(String userId, String sessionId) {
        Optional<ConversationMemory> existing = memoryRepository.findBySessionId(sessionId);
        
        if (existing.isPresent()) {
            return existing.get();
        }

        // Créer une nouvelle mémoire
        ConversationMemory memory = new ConversationMemory(userId, sessionId);
        memory.setMessageHistory(new ArrayList<>());
        memory.setPastRecommendations(new ArrayList<>());
        memory.setContext(new HashMap<>());
        memory.setPreferences(new ConversationMemory.UserPreferences());
        
        return memoryRepository.save(memory);
    }

    /**
     * Ajoute un échange de message à l'historique
     */
    public void addMessageExchange(String sessionId, String role, String content, String action, Map<String, Object> metadata) {
        ConversationMemory memory = getOrCreateConversationMemory("anonymous", sessionId);
        
        ConversationMemory.MessageExchange exchange = new ConversationMemory.MessageExchange();
        exchange.setRole(role);
        exchange.setContent(content);
        exchange.setTimestamp(Instant.now());
        exchange.setAction(action);
        exchange.setMetadata(metadata);
        
        if (memory.getMessageHistory() == null) {
            memory.setMessageHistory(new ArrayList<>());
        }
        
        memory.getMessageHistory().add(exchange);
        
        // Limiter la taille de l'historique
        if (memory.getMessageHistory().size() > MAX_HISTORY_SIZE) {
            memory.getMessageHistory().remove(0);
        }
        
        memoryRepository.save(memory);
        logger.debug("Message ajouté à l'historique pour la session {}", sessionId);
    }

    /**
     * Met à jour le profil client à partir des informations extraites
     */
    public void updateCustomerProfile(String sessionId, RecommendationRequestDTO profile) {
        ConversationMemory memory = getOrCreateConversationMemory("anonymous", sessionId);
        
        ConversationMemory.CustomerProfile customerProfile = memory.getCustomerProfile();
        if (customerProfile == null) {
            customerProfile = new ConversationMemory.CustomerProfile();
        }
        
        // Mettre à jour les champs non-null
        if (profile.getAge() != null) customerProfile.setAge(profile.getAge());
        if (profile.getGender() != null) customerProfile.setGender(profile.getGender());
        if (profile.getMaritalStatus() != null) customerProfile.setMaritalStatus(profile.getMaritalStatus());
        if (profile.getNumberOfChildren() != null) customerProfile.setNumberOfChildren(profile.getNumberOfChildren());
        if (profile.getMedicalNeeds() != null && !profile.getMedicalNeeds().isEmpty()) {
            // Temporairement désactivé - la méthode n'existe pas dans CustomerProfile
        }
        if (profile.getFamilySituation() != null) customerProfile.setFamilySituation(profile.getFamilySituation());
        if (profile.getCareFrequency() != null) customerProfile.setCareFrequency(profile.getCareFrequency());
        
        memory.setCustomerProfile(customerProfile);
        memoryRepository.save(memory);
        logger.debug("Profil client mis à jour pour la session {}", sessionId);
    }

    /**
     * Enregistre les recommandations fournies
     */
    public void saveRecommendationSnapshot(String sessionId, RecommendationResponseDTO response) {
        ConversationMemory memory = getOrCreateConversationMemory("anonymous", sessionId);
        
        ConversationMemory.RecommendationSnapshot snapshot = new ConversationMemory.RecommendationSnapshot();
        snapshot.setTimestamp(Instant.now());
        
        if (response.getTopPacks() != null) {
            List<String> packIds = response.getTopPacks().stream()
                    .map(pack -> pack.getId())
                    .toList();
            snapshot.setRecommendedPackIds(packIds);
        }
        
        if (response.getTopProducts() != null) {
            List<String> productIds = response.getTopProducts().stream()
                    .map(product -> product.getId())
                    .toList();
            snapshot.setRecommendedProductIds(productIds);
        }
        
        // Calculer le score moyen
        double avgScore = 0.0;
        int count = 0;
        if (response.getTopPacks() != null) {
            for (var pack : response.getTopPacks()) {
                avgScore += pack.getCompatibilityScore();
                count++;
            }
        }
        if (response.getTopProducts() != null) {
            for (var product : response.getTopProducts()) {
                avgScore += product.getCompatibilityScore();
                count++;
            }
        }
        if (count > 0) {
            avgScore /= count;
        }
        snapshot.setAverageScore(avgScore);
        
        if (memory.getPastRecommendations() == null) {
            memory.setPastRecommendations(new ArrayList<>());
        }
        
        memory.getPastRecommendations().add(snapshot);
        
        // Limiter l'historique des recommandations
        if (memory.getPastRecommendations().size() > MAX_RECOMMENDATIONS_HISTORY) {
            memory.getPastRecommendations().remove(0);
        }
        
        memoryRepository.save(memory);
        logger.debug("Snapshot de recommandation sauvegardé pour la session {}", sessionId);
    }

    /**
     * Enregistre le feedback utilisateur sur une recommandation
     */
    public void recordUserFeedback(String sessionId, String feedback) {
        ConversationMemory memory = getOrCreateConversationMemory("anonymous", sessionId);
        
        if (memory.getPastRecommendations() != null && !memory.getPastRecommendations().isEmpty()) {
            ConversationMemory.RecommendationSnapshot lastSnapshot = 
                memory.getPastRecommendations().get(memory.getPastRecommendations().size() - 1);
            lastSnapshot.setUserFeedback(feedback);
            memoryRepository.save(memory);
            logger.debug("Feedback utilisateur enregistré pour la session {}", sessionId);
        }
    }

    /**
     * Met à jour les préférences utilisateur déduites
     */
    public void updateUserPreferences(String sessionId, Map<String, Object> preferences) {
        ConversationMemory memory = getOrCreateConversationMemory("anonymous", sessionId);
        
        ConversationMemory.UserPreferences userPreferences = memory.getPreferences();
        if (userPreferences == null) {
            userPreferences = new ConversationMemory.UserPreferences();
        }
        
        // Mettre à jour les préférences
        if (preferences.containsKey("maxMonthlyBudget")) {
            userPreferences.setMaxMonthlyBudget(((Number) preferences.get("maxMonthlyBudget")).doubleValue());
        }
        if (preferences.containsKey("prefersFamilyCoverage")) {
            userPreferences.setPrefersFamilyCoverage((Boolean) preferences.get("prefersFamilyCoverage"));
        }
        if (preferences.containsKey("prefersComprehensiveCoverage")) {
            userPreferences.setPrefersComprehensiveCoverage((Boolean) preferences.get("prefersComprehensiveCoverage"));
        }
        
        memory.setPreferences(userPreferences);
        memoryRepository.save(memory);
        logger.debug("Préférences utilisateur mises à jour pour la session {}", sessionId);
    }

    /**
     * Récupère le contexte de conversation pour un utilisateur
     */
    public Map<String, Object> getConversationContext(String sessionId) {
        Optional<ConversationMemory> memoryOpt = memoryRepository.findBySessionId(sessionId);
        
        if (memoryOpt.isEmpty()) {
            return new HashMap<>();
        }
        
        ConversationMemory memory = memoryOpt.get();
        Map<String, Object> context = new HashMap<>();
        
        context.put("customerProfile", memory.getCustomerProfile());
        context.put("preferences", memory.getPreferences());
        context.put("pastRecommendations", memory.getPastRecommendations());
        context.put("messageHistory", memory.getMessageHistory());
        context.put("context", memory.getContext());
        
        return context;
    }

    /**
     * Récupère l'historique des messages pour une session
     */
    public List<ConversationMemory.MessageExchange> getMessageHistory(String sessionId, int limit) {
        Optional<ConversationMemory> memoryOpt = memoryRepository.findBySessionId(sessionId);
        
        if (memoryOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<ConversationMemory.MessageExchange> history = memoryOpt.get().getMessageHistory();
        if (history == null) {
            return new ArrayList<>();
        }
        
        if (limit > 0 && history.size() > limit) {
            return history.subList(history.size() - limit, history.size());
        }
        
        return history;
    }

    /**
     * Nettoie les anciennes mémoires (plus de 30 jours)
     */
    public void cleanupOldMemories() {
        Instant cutoffDate = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        memoryRepository.deleteOlderThan(cutoffDate);
        logger.info("Nettoyage des anciennes mémoires effectué");
    }

    /**
     * Supprime une mémoire de conversation
     */
    public void deleteConversationMemory(String sessionId) {
        memoryRepository.deleteBySessionId(sessionId);
        logger.info("Mémoire de conversation supprimée pour la session {}", sessionId);
    }

    /**
     * Génère un ID de session unique
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}

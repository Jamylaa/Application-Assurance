package tn.vermeg.gestionproduit.services.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.Garantie;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
// Service Producteur Kafka pour publier les événements métier
 // Supporte les événements: ProductCreated, PackCreated, GarantieCreated, RecommendationGenerated, UserPromptSubmitted
@Service
public class KafkaEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    // Topics Kafka
    private static final String PRODUCT_TOPIC = "product-events";
    private static final String PACK_TOPIC = "pack-events";
    private static final String GARANTIE_TOPIC = "garantie-events";
    private static final String RECOMMENDATION_TOPIC = "recommendation-events";
    private static final String PROMPT_TOPIC = "prompt-events";
    private static final String AI_METRICS_TOPIC = "ai-metrics-events";

    @Autowired
    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
// Publie un événement de création de produit
    public CompletableFuture<SendResult<String, Object>> publishProductCreatedEvent(Produit produit) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ProductCreated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("productId", produit.getIdProduit());
        event.put("productName", produit.getNomProduit());
        event.put("productType", produit.getTypeProduit());
        event.put("description", produit.getDescription());
        event.put("status", produit.getStatut() != null ? produit.getStatut().name() : "UNKNOWN");
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing ProductCreated event for product: {}", produit.getNomProduit());
        return kafkaTemplate.send(PRODUCT_TOPIC, produit.getIdProduit(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("ProductCreated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish ProductCreated event", ex);
                }
            });
    }
// Publie un événement de création de pack
    public CompletableFuture<SendResult<String, Object>> publishPackCreatedEvent(Pack pack) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PackCreated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("packId", pack.getIdPack());
        event.put("packName", pack.getNomPack());
        event.put("description", pack.getDescription());
        event.put("monthlyPrice", pack.getPrixMensuel());
        event.put("ageMinimum", pack.getAgeMinimum());
        event.put("ageMaximum", pack.getAgeMaximum());
        event.put("coverageLevel", pack.getNiveauCouverture());
        event.put("geographicCoverage", pack.getCouvertureGeographique());
        event.put("productId", pack.getProduitId());
        event.put("productName", pack.getNomProduit());
        event.put("status", pack.getStatut() != null ? pack.getStatut().name() : "UNKNOWN");
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing PackCreated event for pack: {}", pack.getNomPack());
        return kafkaTemplate.send(PACK_TOPIC, pack.getIdPack(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("PackCreated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish PackCreated event", ex);
                }
            });
    }
    public CompletableFuture<SendResult<String, Object>> publishGarantieCreatedEvent(Garantie garantie) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "GarantieCreated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("garantieId", garantie.getIdGarantie());
        event.put("garantieName", garantie.getNomGarantie());
        event.put("description", garantie.getDescription());
        event.put("domaineMedical", garantie.getDomaine());
        event.put("reimbursementRate", garantie.getTauxRemboursement());
        event.put("annualCeiling", garantie.getPlafondAnnuel());
        event.put("monthlyCeiling", garantie.getPlafondMensuel());
        event.put("perActCeiling", garantie.getPlafondParActe());
        event.put("deductible", garantie.getFranchise());
        event.put("status", garantie.getStatut() != null ? garantie.getStatut().name() : "UNKNOWN");
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing GarantieCreated event for garantie: {}", garantie.getNomGarantie());
        return kafkaTemplate.send(GARANTIE_TOPIC, garantie.getIdGarantie(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("GarantieCreated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish GarantieCreated event", ex);
                }
            });
    }
    public CompletableFuture<SendResult<String, Object>> publishRecommendationGeneratedEvent(
            String sessionId, String userId, Map<String, Object> recommendationData) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "RecommendationGenerated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("sessionId", sessionId);
        event.put("userId", userId);
        event.put("recommendationData", recommendationData);
        event.put("sourceService", "Recommendation-Service");

        logger.info("Publishing RecommendationGenerated event for session: {}", sessionId);
        return kafkaTemplate.send(RECOMMENDATION_TOPIC, sessionId, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("RecommendationGenerated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish RecommendationGenerated event", ex);
                }
            });
    }
 //Publie un événement de soumission de prompt utilisateur
         public CompletableFuture<SendResult<String, Object>> publishPromptSubmittedEvent(
            String sessionId, String prompt, String actionDetected) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "UserPromptSubmitted");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("sessionId", sessionId);
        event.put("prompt", prompt);
        event.put("actionDetected", actionDetected);
        event.put("sourceService", "AI-Orchestrator-Service");

        logger.info("Publishing UserPromptSubmitted event for session: {}", sessionId);
        return kafkaTemplate.send(PROMPT_TOPIC, sessionId, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("UserPromptSubmitted event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish UserPromptSubmitted event", ex);
                }
            });
    }
// Publie des métriques IA
    public CompletableFuture<SendResult<String, Object>> publishAIMetricsEvent(Map<String, Object> metrics) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "AIMetricsCollected");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("metrics", metrics);
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing AIMetricsCollected event");
        return kafkaTemplate.send(AI_METRICS_TOPIC, "metrics", event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("AIMetricsCollected event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish AIMetricsCollected event", ex);
                }
            });
    }
//Publie un événement de mise à jour de produit
    public CompletableFuture<SendResult<String, Object>> publishProductUpdatedEvent(Produit produit) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ProductUpdated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("productId", produit.getIdProduit());
        event.put("productName", produit.getNomProduit());
        event.put("productType", produit.getTypeProduit());
        event.put("status", produit.getStatut() != null ? produit.getStatut().name() : "UNKNOWN");
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing ProductUpdated event for product: {}", produit.getNomProduit());
        return kafkaTemplate.send(PRODUCT_TOPIC, produit.getIdProduit(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("ProductUpdated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish ProductUpdated event", ex);
                }
            });
    }
// Publie un événement de mise à jour de pack
    public CompletableFuture<SendResult<String, Object>> publishPackUpdatedEvent(Pack pack) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PackUpdated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("packId", pack.getIdPack());
        event.put("packName", pack.getNomPack());
        event.put("monthlyPrice", pack.getPrixMensuel());
        event.put("status", pack.getStatut() != null ? pack.getStatut().name() : "UNKNOWN");
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing PackUpdated event for pack: {}", pack.getNomPack());
        return kafkaTemplate.send(PACK_TOPIC, pack.getIdPack(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("PackUpdated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish PackUpdated event", ex);
                }
            });
    }
// Publie un événement de mise à jour de garantie
    public CompletableFuture<SendResult<String, Object>> publishGarantieUpdatedEvent(Garantie garantie) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "GarantieUpdated");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("garantieId", garantie.getIdGarantie());
        event.put("garantieName", garantie.getNomGarantie());
        event.put("status", garantie.getStatut() != null ? garantie.getStatut().name() : "UNKNOWN");
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing GarantieUpdated event for garantie: {}", garantie.getNomGarantie());
        return kafkaTemplate.send(GARANTIE_TOPIC, garantie.getIdGarantie(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("GarantieUpdated event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish GarantieUpdated event", ex);
                }
            });
    }
//Publie un événement de suppression de produit
    public CompletableFuture<SendResult<String, Object>> publishProductDeletedEvent(String productId, String productName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ProductDeleted");
        event.put("eventId", java.util.UUID.randomUUID().toString());
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("productId", productId);
        event.put("productName", productName);
        event.put("sourceService", "GestionProduit");

        logger.info("Publishing ProductDeleted event for product: {}", productName);
        return kafkaTemplate.send(PRODUCT_TOPIC, productId, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("ProductDeleted event published successfully: {}", result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish ProductDeleted event", ex);
                }
            });
    }

    /**
     * Publie un événement générique
     */
    public CompletableFuture<SendResult<String, Object>> publishGenericEvent(
            String topic, String key, Map<String, Object> eventData) {
        logger.info("Publishing generic event to topic: {}", topic);
        return kafkaTemplate.send(topic, key, eventData)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Generic event published successfully to topic {}: {}", topic, result.getRecordMetadata());
                } else {
                    logger.error("Failed to publish generic event to topic {}", topic, ex);
                }
            });
    }
}

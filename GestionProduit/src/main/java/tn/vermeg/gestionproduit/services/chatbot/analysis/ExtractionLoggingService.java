package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service de logging détaillé pour l'extraction d'entités du chatbot.
 * Génère des logs structurés pour identifier les champs non détectés.
 * 
 * @author PFE Ingénieur - GestionProduit
 * @version 2.0 - Enhanced AI Analysis Engine
 */
@Service
public class ExtractionLoggingService {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionLoggingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Journal d'extraction pour un prompt
     */
    public static class ExtractionLog {
        private final String sessionId;
        private final String timestamp;
        private final String originalPrompt;
        private final String normalizedPrompt;
        private String action;
        private final Map<String, FieldExtractionResult> fieldResults;
        private final List<String> warnings;
        private final List<String> errors;
        private double confidenceScore;
        private final Map<String, Object> metadata;

        public ExtractionLog(String sessionId, String originalPrompt, String normalizedPrompt, String action) {
            this.sessionId = sessionId;
            this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            this.originalPrompt = originalPrompt;
            this.normalizedPrompt = normalizedPrompt;
            this.action = action;
            this.fieldResults = new LinkedHashMap<>();
            this.warnings = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.confidenceScore = 0.0;
            this.metadata = new HashMap<>();
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getTimestamp() { return timestamp; }
        public String getOriginalPrompt() { return originalPrompt; }
        public String getNormalizedPrompt() { return normalizedPrompt; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Map<String, FieldExtractionResult> getFieldResults() { return fieldResults; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        public double getConfidenceScore() { return confidenceScore; }
        public Map<String, Object> getMetadata() { return metadata; }

        public void addFieldResult(String fieldName, FieldExtractionResult result) {
            fieldResults.put(fieldName, result);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addError(String error) {
            errors.add(error);
        }

        public void setConfidenceScore(double score) {
            this.confidenceScore = score;
        }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }

    /**
     * Résultat d'extraction pour un champ spécifique
     */
    public static class FieldExtractionResult {
        private final String fieldName;
        private final Object extractedValue;
        private final String extractionMethod;
        private final boolean detected;
        private final double confidence;
        private final String source; // "AI", "REGEX", "DEFAULT", "MERGED"
        private final String rawMatch;

        public FieldExtractionResult(String fieldName, Object extractedValue, String extractionMethod,
                                   boolean detected, double confidence, String source, String rawMatch) {
            this.fieldName = fieldName;
            this.extractedValue = extractedValue;
            this.extractionMethod = extractionMethod;
            this.detected = detected;
            this.confidence = confidence;
            this.source = source;
            this.rawMatch = rawMatch;
        }

        // Getters
        public String getFieldName() { return fieldName; }
        public Object getExtractedValue() { return extractedValue; }
        public String getExtractionMethod() { return extractionMethod; }
        public boolean isDetected() { return detected; }
        public double getConfidence() { return confidence; }
        public String getSource() { return source; }
        public String getRawMatch() { return rawMatch; }
    }

    private final Map<String, ExtractionLog> sessionLogs = new HashMap<>();

    /**
     * Crée un nouveau journal d'extraction pour une session
     */
    public ExtractionLog createExtractionLog(String sessionId, String originalPrompt, 
                                            String normalizedPrompt, String action) {
        ExtractionLog log = new ExtractionLog(sessionId, originalPrompt, normalizedPrompt, action);
        sessionLogs.put(sessionId, log);
        logger.info("[ExtractionLog] Created log for session: {}, action: {}", sessionId, action);
        return log;
    }

    /**
     * Log l'extraction d'un champ
     */
    public void logFieldExtraction(ExtractionLog log, String fieldName, Object value, 
                                 String method, double confidence, String source, String rawMatch) {
        boolean detected = value != null && !(value instanceof String && ((String) value).isBlank());
        
        FieldExtractionResult result = new FieldExtractionResult(
            fieldName, value, method, detected, confidence, source, rawMatch
        );
        
        log.addFieldResult(fieldName, result);
        
        if (detected) {
            logger.debug("[ExtractionLog] Field '{}' detected: {} (method: {}, confidence: {}, source: {})",
                       fieldName, value, method, confidence, source);
        } else {
            logger.warn("[ExtractionLog] Field '{}' NOT detected (method: {}, source: {})",
                       fieldName, method, source);
        }
    }

    /**
     * Génère un rapport résumé des champs non détectés
     */
    public String generateUndetectedFieldsReport(ExtractionLog log) {
        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT D'EXTRACTION ===\n");
        report.append(String.format("Session: %s\n", log.getSessionId()));
        report.append(String.format("Timestamp: %s\n", log.getTimestamp()));
        report.append(String.format("Action: %s\n", log.getAction()));
        report.append(String.format("Confidence Score: %.2f\n\n", log.getConfidenceScore()));
        
        report.append("--- CHAMPS NON DÉTECTÉS ---\n");
        List<String> undetected = new ArrayList<>();
        for (Map.Entry<String, FieldExtractionResult> entry : log.getFieldResults().entrySet()) {
            if (!entry.getValue().isDetected()) {
                undetected.add(entry.getKey());
            }
        }
        
        if (undetected.isEmpty()) {
            report.append("Tous les champs ont été détectés.\n");
        } else {
            for (String field : undetected) {
                FieldExtractionResult result = log.getFieldResults().get(field);
                report.append(String.format("- %s (méthode: %s, source: %s)\n",
                    field, result.getExtractionMethod(), result.getSource()));
            }
        }
        
        report.append("\n--- CHAMPS DÉTECTÉS ---\n");
        List<String> detected = new ArrayList<>();
        for (Map.Entry<String, FieldExtractionResult> entry : log.getFieldResults().entrySet()) {
            if (entry.getValue().isDetected()) {
                detected.add(entry.getKey());
            }
        }
        
        for (String field : detected) {
            FieldExtractionResult result = log.getFieldResults().get(field);
            report.append(String.format("- %s: %s (confiance: %.2f, source: %s)\n",
                field, result.getExtractedValue(), result.getConfidence(), result.getSource()));
        }
        
        if (!log.getWarnings().isEmpty()) {
            report.append("\n--- AVERTISSEMENTS ---\n");
            for (String warning : log.getWarnings()) {
                report.append(String.format("- %s\n", warning));
            }
        }
        
        if (!log.getErrors().isEmpty()) {
            report.append("\n--- ERREURS ---\n");
            for (String error : log.getErrors()) {
                report.append(String.format("- %s\n", error));
            }
        }
        
        return report.toString();
    }

    /**
     * Log le rapport complet dans les logs de l'application
     */
    public void logFullReport(ExtractionLog log) {
        String report = generateUndetectedFieldsReport(log);
        logger.info("\n{}", report);
    }

    /**
     * Récupère le journal d'extraction pour une session
     */
    public ExtractionLog getExtractionLog(String sessionId) {
        return sessionLogs.get(sessionId);
    }

    /**
     * Supprime le journal d'extraction pour une session
     */
    public void clearExtractionLog(String sessionId) {
        sessionLogs.remove(sessionId);
    }

    /**
     * Calcule le score de confiance global basé sur les résultats d'extraction
     */
    public double calculateGlobalConfidence(ExtractionLog log) {
        if (log.getFieldResults().isEmpty()) {
            return 0.0;
        }
        
        double totalConfidence = 0.0;
        int detectedCount = 0;
        
        for (FieldExtractionResult result : log.getFieldResults().values()) {
            totalConfidence += result.getConfidence();
            if (result.isDetected()) {
                detectedCount++;
            }
        }
        
        double averageConfidence = totalConfidence / log.getFieldResults().size();
        double detectionRate = (double) detectedCount / log.getFieldResults().size();
        
        // Ponderation: 70% pour la détection, 30% pour la confiance moyenne
        return (detectionRate * 0.7) + (averageConfidence * 0.3);
    }

    /**
     * Identifie les champs critiques manquants
     */
    public List<String> identifyCriticalMissingFields(ExtractionLog log, String action) {
        List<String> criticalFields = new ArrayList<>();
        
        // Champs critiques par action
        Set<String> requiredByAction = switch (action.toUpperCase()) {
            case "GARANTIE" -> Set.of("nomGarantie", "tauxRemboursement");
            case "PRODUIT" -> Set.of("nomProduit", "typeProduit");
            case "PACK" -> Set.of("nomPack", "prixMensuel", "ageMinimum", "ageMaximum");
            case "CONFIGURATION_PACK" -> Set.of("packId", "garantieId");
            default -> Set.of();
        };
        
        for (String field : requiredByAction) {
            FieldExtractionResult result = log.getFieldResults().get(field);
            if (result == null || !result.isDetected()) {
                criticalFields.add(field);
            }
        }
        
        return criticalFields;
    }

    /**
     * Génère des suggestions pour améliorer l'extraction
     */
    public List<String> generateImprovementSuggestions(ExtractionLog log) {
        List<String> suggestions = new ArrayList<>();
        
        double confidence = log.getConfidenceScore();
        
        if (confidence < 0.5) {
            suggestions.add("Confiance faible: reformulez le prompt avec plus de détails explicites");
        }
        
        List<String> criticalMissing = identifyCriticalMissingFields(log, log.getAction());
        if (!criticalMissing.isEmpty()) {
            suggestions.add("Champs critiques manquants: " + String.join(", ", criticalMissing));
        }
        
        // Vérifier si l'IA n'a pas été utilisée
        boolean aiUsed = log.getFieldResults().values().stream()
            .anyMatch(r -> "AI".equals(r.getSource()));
        
        if (!aiUsed) {
            suggestions.add("L'IA n'a pas été utilisée pour l'extraction: vérifiez la configuration de l'API Gemini");
        }
        
        // Vérifier les champs avec faible confiance
        List<String> lowConfidenceFields = log.getFieldResults().entrySet().stream()
            .filter(e -> e.getValue().isDetected() && e.getValue().getConfidence() < 0.6)
            .map(Map.Entry::getKey)
            .toList();
        
        if (!lowConfidenceFields.isEmpty()) {
            suggestions.add("Champs avec faible confiance: " + String.join(", ", lowConfidenceFields));
        }
        
        return suggestions;
    }
}

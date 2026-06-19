package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service de normalisation NLP pour le prétraitement des prompts du chatbot.
 * Effectue la normalisation textuelle avant l'extraction d'entités.
 * 
 * @author PFE Ingénieur - GestionProduit
 * @version 2.0 - Enhanced AI Analysis Engine
 */
@Service
public class NLPNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(NLPNormalizationService.class);

    // Patterns pour la normalisation
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9àâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\s.,;:!?\\-\\']");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_PUNCTUATION_PATTERN = Pattern.compile("[.,;:!?]{2,}");

    /**
     * Résultat de la normalisation NLP avec métadonnées
     */
    public static class NormalizationResult {
        private final String normalizedText;
        private final String originalText;
        private final Map<String, Object> metadata;
        private final List<String> removedSegments;
        private final List<String> recognizedEnums;

        public NormalizationResult(String normalizedText, String originalText, Map<String, Object> metadata,
                                   List<String> removedSegments, List<String> recognizedEnums) {
            this.normalizedText = normalizedText;
            this.originalText = originalText;
            this.metadata = metadata;
            this.removedSegments = removedSegments;
            this.recognizedEnums = recognizedEnums;
        }

        public String getNormalizedText() { return normalizedText; }
        public String getOriginalText() { return originalText; }
        public Map<String, Object> getMetadata() { return metadata; }
        public List<String> getRemovedSegments() { return removedSegments; }
        public List<String> getRecognizedEnums() { return recognizedEnums; }
    }

    /**
     * Normalise le texte du prompt pour l'extraction d'entités
     */
    public NormalizationResult normalizePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new NormalizationResult("", prompt, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<String> removedSegments = new ArrayList<>();
        List<String> recognizedEnums = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        String normalized = prompt;

        // Étape 1: Suppression des caractères spéciaux
        normalized = removeSpecialCharacters(normalized);
        metadata.put("specialCharsRemoved", !normalized.equals(prompt));

        // Étape 2: Normalisation des espaces
        normalized = normalizeSpaces(normalized);

        // Étape 3: Normalisation de la ponctuation
        normalized = normalizePunctuation(normalized);

        // Étape 4: Reconnaissance des enums
        recognizedEnums = recognizeEnums(normalized);
        metadata.put("recognizedEnums", recognizedEnums);

        // Étape 5: Segmentation des garanties
        Map<String, List<String>> segments = segmentGuarantees(normalized);
        metadata.put("guaranteeSegments", segments);

        // Étape 6: Normalisation des nombres
        normalized = normalizeNumbers(normalized);

        // Étape 7: Normalisation des devises
        normalized = normalizeCurrencies(normalized);

        // Étape 8: Expansion des abréviations courantes
        normalized = expandAbbreviations(normalized);
        metadata.put("abbreviationsExpanded", true);

        logger.debug("NLP Normalization - Original length: {}, Normalized length: {}", 
                    prompt.length(), normalized.length());
        logger.debug("Recognized enums: {}", recognizedEnums);

        return new NormalizationResult(normalized, prompt, metadata, removedSegments, recognizedEnums);
    }

    /**
     * Supprime les caractères spéciaux tout en préservant les caractères accentués français
     */
    private String removeSpecialCharacters(String text) {
        return SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Normalise les espaces multiples en espaces simples
     */
    private String normalizeSpaces(String text) {
        return MULTIPLE_SPACES_PATTERN.matcher(text).replaceAll(" ").trim();
    }

    /**
     * Normalise la ponctuation multiple en ponctuation simple
     */
    private String normalizePunctuation(String text) {
        return MULTIPLE_PUNCTUATION_PATTERN.matcher(text).replaceAll(".");
    }

    /**
     * Reconnaît les enums dans le texte
     */
    private List<String> recognizeEnums(String text) {
        List<String> enums = new ArrayList<>();
        String upperText = text.toUpperCase();

        // Enums de TypeProduit
        if (upperText.contains("SANTE") || upperText.contains("SANTÉ")) enums.add("TypeProduit:SANTE");
        if (upperText.contains("AUTO")) enums.add("TypeProduit:AUTO");
        if (upperText.contains("HABITATION")) enums.add("TypeProduit:HABITATION");
        if (upperText.contains("VIE")) enums.add("TypeProduit:VIE");
        if (upperText.contains("EPARGNE") || upperText.contains("ÉPARGNE")) enums.add("TypeProduit:EPARGNE");

        // Enums de NiveauCouverture
        if (upperText.contains("BASIC") || upperText.contains("BRONZE")) enums.add("NiveauCouverture:BASIC");
        if (upperText.contains("PREMIUM") || upperText.contains("GOLD")) enums.add("NiveauCouverture:PREMIUM");

        // Enums de CouvertureGeographique
        if (upperText.contains("INTERNATIONAL")) enums.add("CouvertureGeographique:INTERNATIONAL");
        if (upperText.contains("UE") || upperText.contains("EUROPE")) enums.add("CouvertureGeographique:UE");
        if (upperText.contains("MAGHREB")) enums.add("CouvertureGeographique:MAGHREB");
        if (upperText.contains("NATIONAL")) enums.add("CouvertureGeographique:NATIONAL");

        // Enums de TypeClient
        if (upperText.contains("INDIVIDUEL")) enums.add("TypeClient:INDIVIDUEL");
        if (upperText.contains("FAMILLE")) enums.add("TypeClient:FAMILLE");
        if (upperText.contains("SENIOR")) enums.add("TypeClient:SENIOR");
        if (upperText.contains("ENTREPRISE")) enums.add("TypeClient:ENTREPRISE");

        // Enums de Statut
        if (upperText.contains("ACTIF")) enums.add("Statut:ACTIF");
        if (upperText.contains("INACTIF")) enums.add("Statut:INACTIF");

        return enums;
    }

    /**
     * Segmente les garanties du texte
     */
    private Map<String, List<String>> segmentGuarantees(String text) {
        Map<String, List<String>> segments = new HashMap<>();
        
        // Délimiteurs de segmentation pour les garanties
        String[] delimiters = {",", ";", "et", "ainsi que", "ainsi que", "incluant", "inclut", "avec", "comprend"};
        String[] parts = text.split("(?i)(?:,|;|\\bet\\b|\\bainsi que\\b|\\bincluant\\b|\\binclut\\b|\\bavec\\b|\\bcomprend\\b)");
        
        List<String> guaranteeSegments = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() > 3 && 
                (trimmed.toLowerCase().contains("garantie") || 
                 trimmed.toLowerCase().contains("couverture") ||
                 trimmed.toLowerCase().contains("hospitalisation") ||
                 trimmed.toLowerCase().contains("consultation") ||
                 trimmed.toLowerCase().contains("dentaire") ||
                 trimmed.toLowerCase().contains("optique") ||
                 trimmed.toLowerCase().contains("médicament"))) {
                guaranteeSegments.add(trimmed);
            }
        }
        
        segments.put("garanties", guaranteeSegments);
        return segments;
    }

    /**
     * Normalise les nombres (virgules en points, espaces supprimés)
     */
    private String normalizeNumbers(String text) {
        // Remplacer les virgules décimales par des points
        text = text.replaceAll("(\\d+)[,](\\d+)", "$1.$2");
        // Supprimer les espaces dans les nombres
        text = text.replaceAll("(\\d+)\\s+(\\d)", "$1$2");
        return text;
    }

    /**
     * Normalise les devises
     */
    private String normalizeCurrencies(String text) {
        // Normaliser les symboles de devise
        text = text.replaceAll("€", "euros");
        text = text.replaceAll("\\$", "dollars");
        text = text.replaceAll("TND", "dinars tunisiens");
        return text;
    }

    /**
     * Étend les abréviations courantes
     */
    private String expandAbbreviations(String text) {
        Map<String, String> abbreviations = new HashMap<>();
        abbreviations.put("\\bmin\\b", "minimum");
        abbreviations.put("\\bmax\\b", "maximum");
        abbreviations.put("\\bapprox\\b", "environ");
        abbreviations.put("\\bvs\\b", "versus");
        abbreviations.put("\\betc\\b", "et cetera");
        abbreviations.put("\\bi\\.e\\.", "c'est-à-dire");
        abbreviations.put("\\be\\.g\\.", "par exemple");

        String result = text;
        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Extrait et normalise les phrases naturelles pour les durées
     * Ex: "durée comprise entre 12 et 60 mois" → "dureeMin:12, dureeMax:60"
     */
    public Map<String, Object> extractDurationPhrases(String text) {
        Map<String, Object> durations = new HashMap<>();
        
        // Pattern pour "entre X et Y mois/ans"
        java.util.regex.Pattern betweenPattern = java.util.regex.Pattern.compile(
            "(?:durée|duree|duration|période|periode)\\s+(?:comprise\\s+)?entre\\s+(\\d+)\\s+et\\s+(\\d+)\\s+(mois|ans|years|months)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = betweenPattern.matcher(text);
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            String unit = matcher.group(3).toLowerCase();
            
            if (unit.contains("an")) {
                min *= 12;
                max *= 12;
            }
            
            durations.put("dureeMinContrat", min);
            durations.put("dureeMaxContrat", max);
            durations.put("extractionMethod", "natural_language_between");
        }
        
        // Pattern pour "à partir de X ans"
        java.util.regex.Pattern fromPattern = java.util.regex.Pattern.compile(
            "(?:à partir de|à|depuis)\\s+(\\d+)\\s+(ans|mois)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        matcher = fromPattern.matcher(text);
        if (matcher.find()) {
            int age = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            
            if (unit.contains("an")) {
                durations.put("ageMinimum", age);
            } else {
                durations.put("ancienneteMinMois", age);
            }
            durations.put("extractionMethod", "natural_language_from");
        }
        
        return durations;
    }

    /**
     * Calcule un score de confiance pour l'extraction basé sur la normalisation
     */
    public double calculateConfidenceScore(NormalizationResult result) {
        double score = 1.0;
        
        // Pénalité pour beaucoup de caractères spéciaux supprimés
        if ((boolean) result.getMetadata().getOrDefault("specialCharsRemoved", false)) {
            score -= 0.05;
        }
        
        // Bonus pour reconnaissance d'enums
        int enumCount = result.getRecognizedEnums().size();
        score += Math.min(enumCount * 0.05, 0.2);
        
        // Bonus pour segmentation de garanties
        @SuppressWarnings("unchecked")
        Map<String, List<String>> segments = (Map<String, List<String>>) result.getMetadata().get("guaranteeSegments");
        if (segments != null && !segments.get("garanties").isEmpty()) {
            score += 0.1;
        }
        
        return Math.min(Math.max(score, 0.0), 1.0);
    }
}

package tn.vermeg.gestionproduit.services.chatbot.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(AIExtractionService.class);

    @Value("${gemini.api-key:}")
    private String googleApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${gemini.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiUrl;

    @Value("${gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${gemini.max-retries:3}")
    private int maxRetries;

    @Value("${gemini.retry-delay-ms:1000}")
    private int retryDelayMs;

    @Value("${gemini.enabled:true}")
    private boolean geminiEnabled;

    @Value("${chatbot.ai-extraction-enabled:true}")
    private boolean aiExtractionEnabled;

    @Value("${chatbot.fallback-on-ai-error:true}")
    private boolean fallbackOnError;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AIExtractionService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    // ========== EXTRACTION DONNÉES ==========
    public Map<String, Object> extractGarantieData(String prompt) {
        try {
            logger.info("=== EXTRACTION IA GARANTIE ===");
            logger.info("Prompt: {}", prompt);
            
            String enhancedPrompt = prompt + "\n\nExtrais les informations pour créer une garantie d'assurance. IMPORTANT: " +
                "- Retourne un JSON avec: nom, description, domaine (domaine médical parmi: CONSULTATION_GENERALE, CARDIOLOGIE, DENTAIRE, OPHTALMOLOGIE, HOSPITALISATION, etc.), " +
                "- tauxRemboursement (en décimal, ex: 0.8 pour 80%), " +
                "- typeMontant (TARIF_CONVENTIONNE/FRAIS_REELS/FORFAIT - EXACTEMENT ces valeurs, pas de variations), " +
                "- plafondAnnuel, plafondMensuel, plafondParActe, " +
                "- franchise, coutMoyenParSinistre, " +
                "- dureeMinContrat, dureeMaxContrat (DEUX valeurs distinctes, important!), " +
                "- resiliableAnnuellement (true/false), " +
                "- statut (ACTIF/INACTIF). " +
                "- Pour les plages de durée (ex: '12 à 36 mois'), séparez en dureeMinContrat=12 et dureeMaxContrat=36. " +
                "- Utilise null si vraiment absent.";
            
            Map<String, Object> result = callGoogleAIWithRetry(enhancedPrompt);
            
            logger.info("Résultat IA brut: {}", result);
            
            // Validation de la réponse IA pour les champs critiques
            if (result.containsKey("typeMontant")) {
                String typeMontant = (String) result.get("typeMontant");
                logger.info("TypeMontant extrait par IA: {}", typeMontant);
                if (!Arrays.asList("TARIF_CONVENTIONNE", "FRAIS_REELS", "FORFAIT").contains(typeMontant)) {
                    logger.warn("TypeMontant invalide extrait: {}, attendu: TARIF_CONVENTIONNE/FRAIS_REELS/FORFAIT", typeMontant);
                }
            } else {
                logger.warn("TypeMontant non extrait par l'IA");
            }
            
            if (result.containsKey("dureeMinContrat") && result.containsKey("dureeMaxContrat")) {
                logger.info("Durées extraites par IA - Min: {}, Max: {}", result.get("dureeMinContrat"), result.get("dureeMaxContrat"));
            } else {
                logger.warn("Durées min/max non extraites correctement par l'IA");
            }
            
            if (result.containsKey("coutMoyenParSinistre")) {
                logger.info("Coût moyen par sinistre extrait: {}", result.get("coutMoyenParSinistre"));
            } else {
                logger.warn("Coût moyen par sinistre non extrait par l'IA");
            }
            
            if (result.containsKey("resiliableAnnuellement")) {
                logger.info("Résiliable annuellement extrait: {}", result.get("resiliableAnnuellement"));
            } else {
                logger.warn("Résiliable annuellement non extrait par l'IA");
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Erreur extraction garantie IA: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractProduitData(String prompt) {
        try {
            return callGoogleAIWithRetry(prompt + "\n\nExtrais les informations pourcreer un produit d'assurance. Retourne un JSON avec: nom, description, typeProduit (SANTE/AUTO/HABITATION/VIE/PREVOYANCE/EPARGNE), statut. Utilise null si absent.");
        } catch (Exception e) {
            logger.error("Erreur extraction produit IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractPackData(String prompt) {
        try {
            String enhancedPrompt = prompt + "\n\nExtrais les informations pour créer un pack d'assurance. IMPORTANT: " +
                "- Retourne un JSON avec: nom, description, ageMin, ageMax, typeClients (INDIVIDUEL/FAMILLE/SENIOR), couvertureGeographique, prixMensuel (nombre, pas de texte), dureeMinContrat, dureeMaxContrat, niveauCouverture (BASIC/PREMIUM/GOLD), statut (ACTIF/INACTIF), nomProduit (nom du produit associé, pas l'ID). " +
                "- Pour prixMensuel: cherche spécifiquement des chiffres suivis de € ou 'euros' ou 'TND'. Si aucun prix n'est mentionné, utilise 0. " +
                "- Pour typeClients: déduis du nom du pack (ex: 'Famille' → FAMILLE, 'Senior' → SENIOR, sinon INDIVIDUEL). " +
                "- Pour niveauCouverture: déduis du nom (ex: 'Essentiel' → BASIC, 'Premium' → PREMIUM, 'Gold' → GOLD). " +
                "- Pour nomProduit: cherche le nom du produit mentionné (ex: 'associé à Assurance Santé Junior' → nomProduit: 'Assurance Santé Junior'). " +
                "- Utilise null si vraiment absent.";
            Map<String, Object> result = callGoogleAIWithRetry(enhancedPrompt);
            return applyPackFallbackLogic(result, prompt);
        } catch (Exception e) {
            logger.error("Erreur extraction pack IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractPackConfigurationData(String prompt) {
        try {
            return callGoogleAIWithRetry(prompt + "\n\nExtrais les informations pour configurer un pack: packId, garantieId, tauxRemboursement, plafond, franchise, optionnelle, supplementPrix. Utilise null si absent.");
        } catch (Exception e) {
            logger.error("Erreur extraction configuration pack IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractAddGarantieToPackData(String prompt) {
        try {
            Map<String, Object> result = callGoogleAIWithRetry(prompt + "\n\nExtrais: nomPack, nomGarantie, tauxRemboursement, plafond, franchise, optionnelle. Utilise null si absent.");
            // Si l'IA n'a rien retourné ou que le nomPack est manquant, utiliser le fallback regex
            if (result == null || result.isEmpty() || !result.containsKey("nomPack") || result.get("nomPack") == null) {
                logger.warn("IA n'a pas extrait nomPack, utilisation du fallback regex");
                return extractAddGarantieToPackDataFallback(prompt);
            }
            return result;
        } catch (Exception e) {
            logger.error("Erreur extraction ajout garantie pack IA: {}", e.getMessage());
            return extractAddGarantieToPackDataFallback(prompt);
        }
    }

    // ========== APPEL GEMINI AVEC RETRY ROBUSTE ==========
    private Map<String, Object> callGoogleAIWithRetry(String fullPrompt) throws IOException, InterruptedException {
        if (!isAIAvailable()) {
            logger.warn("API KEY non configuree, fallback patterns");
            return new HashMap<>();
        }
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                logger.debug("Tentative {}/{} Gemini", attempt + 1, maxRetries);
                Map<String, Object> result = callGoogleAI(fullPrompt);
                if (!result.isEmpty()) {
                    logger.info("Extraction IA reussie");
                    return result;
                }
            } catch (Exception e) {
                logger.warn("Tentative {} echouee: {}", attempt + 1, e.getMessage());
                attempt++;
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs);
                }
            }
        }

        if (fallbackOnError) {
            logger.warn("Fallback patterns active");
            return new HashMap<>();
        }
        return new HashMap<>();
    }

    private Map<String, Object> callGoogleAI(String fullPrompt) throws IOException, InterruptedException {
        String url = String.format("%s/%s:generateContent?key=%s", geminiUrl, geminiModel, googleApiKey);
        logger.debug("Appel Gemini: {}", geminiModel);

        String requestBody = String.format("""
            {
              "contents": [{"parts": [{"text": "%s"}]}],
              "generationConfig": {
                "temperature": 0.3,
                "maxOutputTokens": 8192
              }
            }
            """, escapeJsonString(fullPrompt));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("Gemini error {}: {}", response.statusCode(), response.body());
            throw new IOException("Gemini error: " + response.statusCode());
        }
        return parseAIResponse(response.body());
    }
    private String escapeJsonString(String str) {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
    private Map<String, Object> parseAIResponse(String aiResponse) {
        try {
            Pattern jsonPattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(aiResponse);
            if (matcher.find()) {
                Map<String, Object> result = objectMapper.readValue(cleanJsonString(matcher.group()), HashMap.class);
                logger.debug("JSON parsed");
                return result;
            }
            logger.warn("Aucun JSON trouve");
            return new HashMap<>();
        } catch (Exception e) {
            logger.warn("Parse error: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    private String cleanJsonString(String jsonStr) {
        jsonStr = jsonStr.replaceAll("(?<![\\\\])'([^']*)'", "\"$1\"");
        jsonStr = jsonStr.replaceAll("[\\x00-\\x1F]", "");
        return jsonStr;
    }
    // ========== HELPERS ==========
    public String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? defaultValue : s;
    }
    public double getDoubleValue(Map<String, Object> data, String key, double defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getIntegerValue(Map<String, Object> data, String key, int defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBooleanValue(Map<String, Object> data, String key, boolean defaultValue) {
        if (data == null) return defaultValue;
        Object value = data.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean isAIAvailable() {
        return googleApiKey != null && !googleApiKey.trim().isEmpty() && geminiEnabled && aiExtractionEnabled;
    }

    // ========== LOGIQUE DE FALLBACK POUR LES PACKS ==========
    private Map<String, Object> applyPackFallbackLogic(Map<String, Object> data, String originalPrompt) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        String packName = getStringValue(data, "nom", "").toLowerCase();
        String promptLower = originalPrompt.toLowerCase();

        // Déduire le niveau de couverture à partir du nom
        if (data.get("niveauCouverture") == null || data.get("niveauCouverture").toString().trim().isEmpty()) {
            String niveau = deduceCoverageLevel(packName, promptLower);
            if (niveau != null) {
                data.put("niveauCouverture", niveau);
                logger.info("Niveau de couverture déduit: {}", niveau);
            }
        }

        // Déduire le type de client à partir du nom
        if (data.get("typeClients") == null || data.get("typeClients").toString().trim().isEmpty()) {
            String typeClient = deduceClientType(packName, promptLower);
            if (typeClient != null) {
                data.put("typeClients", typeClient);
                logger.info("Type de client déduit: {}", typeClient);
            }
        }

        // Extraire le prix avec regex si non trouvé par l'IA
        if (data.get("prixMensuel") == null || getDoubleValue(data, "prixMensuel", -1) <= 0) {
            Double prix = extractPriceFromPrompt(promptLower);
            if (prix != null && prix > 0) {
                data.put("prixMensuel", prix);
                logger.info("Prix extrait: {}", prix);
            }
        }

        return data;
    }

    private String deduceCoverageLevel(String packName, String prompt) {
        if (packName.contains("essentiel") || packName.contains("basic") || packName.contains("confort") || packName.contains("standard") || prompt.contains("essentiel") || prompt.contains("basic") || prompt.contains("confort") || prompt.contains("standard")) {
            return "BASIC";
        } else if (packName.contains("premium") || packName.contains("gold") || prompt.contains("premium") || prompt.contains("gold")) {
            return "PREMIUM";
        }
        return null; // Ne pas appliquer de valeur par défaut, laisser null pour validation
    }

    private String deduceClientType(String packName, String prompt) {
        if (packName.contains("famille") || packName.contains("enfants") || prompt.contains("famille") || prompt.contains("enfants")) {
            return "FAMILLE";
        } else if (packName.contains("senior") || packName.contains("60") || prompt.contains("senior") || prompt.contains("60 ans")) {
            return "SENIOR";
        }
        return "INDIVIDUEL"; // Valeur par défaut
    }

    private Double extractPriceFromPrompt(String prompt) {
        // Cherche des prix comme: "50€", "50 euros", "50 TND", "prix 50"
        Pattern pricePattern = Pattern.compile("(?:prix\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros|eur|TND|tunisien)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pricePattern.matcher(prompt);
        if (matcher.find()) {
            try {
                String priceStr = matcher.group(1).replace(",", ".");
                return Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing prix: {}", e.getMessage());
            }
        }
        return null;
    }

    // ========== FALLBACK REGEX POUR AJOUT GARANTIE AU PACK ==========
    private Map<String, Object> extractAddGarantieToPackDataFallback(String prompt) {
        Map<String, Object> result = new HashMap<>();
        String promptLower = prompt.toLowerCase();

        // Extraire le nom du pack: "au pack X" ou "pack X"
        Pattern packPattern = Pattern.compile("(?:au\\s+pack|pack\\s+)([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\s]+?)(?:\\s+avec|\\s+et|,|\\.$|$)", Pattern.CASE_INSENSITIVE);
        Matcher packMatcher = packPattern.matcher(prompt);
        if (packMatcher.find()) {
            String packName = packMatcher.group(1).trim();
            result.put("nomPack", packName);
            logger.info("nomPack extrait via fallback: {}", packName);
        }
        // Extraire le nom de la garantie: "la garantie X" ou "garantie X"
        Pattern garantiePattern = Pattern.compile("(?:la\\s+garantie|garantie\\s+)([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\s]+?)(?:\\s+avec|\\s+et|\\s+un|,|\\.$|$)", Pattern.CASE_INSENSITIVE);
        Matcher garantieMatcher = garantiePattern.matcher(prompt);
        if (garantieMatcher.find()) {
            String garantieName = garantieMatcher.group(1).trim();
            result.put("nomGarantie", garantieName);
            logger.info("nomGarantie extrait via fallback: {}", garantieName);
        }

        // Extraire le taux de remboursement: "90 pourcent", "90%", "90 pour cent"
        Pattern tauxPattern = Pattern.compile("(?:taux\\s+de\\s+remboursement\\s*(?:de|:)?\\s*|remboursement\\s*(?:de|:)?\\s*)?(\\d+)\\s*(?:%|pourcent|pour\\s+cent)", Pattern.CASE_INSENSITIVE);
        Matcher tauxMatcher = tauxPattern.matcher(prompt);
        if (tauxMatcher.find()) {
            try {
                double taux = Double.parseDouble(tauxMatcher.group(1)) / 100.0;
                result.put("tauxRemboursement", taux);
                logger.info("tauxRemboursement extrait via fallback: {}", taux);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing taux: {}", e.getMessage());
            }
        }

        // Extraire le plafond: "plafond de 50000", "plafond 50000"
        Pattern plafondPattern = Pattern.compile("(?:plafond\\s*(?:de|:)?\\s*|plafonds?\\s*(?:de|:)?\\s*)(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher plafondMatcher = plafondPattern.matcher(prompt);
        if (plafondMatcher.find()) {
            try {
                double plafond = Double.parseDouble(plafondMatcher.group(1));
                result.put("plafond", plafond);
                logger.info("plafond extrait via fallback: {}", plafond);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing plafond: {}", e.getMessage());
            }
        }

        // Extraire la franchise: "franchise de 100", "franchise 100"
        Pattern franchisePattern = Pattern.compile("(?:franchise\\s*(?:de|:)?\\s*)(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher franchiseMatcher = franchisePattern.matcher(prompt);
        if (franchiseMatcher.find()) {
            try {
                double franchise = Double.parseDouble(franchiseMatcher.group(1));
                result.put("franchise", franchise);
                logger.info("franchise extraite via fallback: {}", franchise);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing franchise: {}", e.getMessage());
            }
        }

        // Déterminer si optionnelle
        if (promptLower.contains("optionnelle") || promptLower.contains("optionnel")) {
            result.put("optionnelle", true);
        }

        return result;
    }
}

/**
 * Service d'extraction IA pour le chatbot avec API Google Gemini.
 * CARACTÉRISTIQUES:
 * - Intégration Google Gemini avec fallback robuste
 * - Timeout configurable avec retry intelligent
 * - Gestion complète des erreurs API
 * - Option de fallback vers extraction par patterns
 * - Logs fournis pour débogage
 * - Jamais null - toujours un Map<> vide en fallback
 * CONFIG (application.yml):
 * gemini:
 *   api-key: ...
 *   model: gemini-2.0-flash (VALIDE!)
 *   url: https://generativelanguage.googleapis.com/v1beta/models
 *   timeout-seconds: 30
 *   max-retries: 3
 *   retry-delay-ms: 1000
 */

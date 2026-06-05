package tn.vermeg.gestionproduit.services.chatbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.HashMap;
import java.util.Map;

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
            return callGoogleAIWithRetry(buildExtractionPrompt("GARANTIE", prompt));
        } catch (Exception e) {
            logger.error("❌ Erreur extraction garantie IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractProduitData(String prompt) {
        try {
            return callGoogleAIWithRetry(buildExtractionPrompt("PRODUIT", prompt));
        } catch (Exception e) {
            logger.error("❌ Erreur extraction produit IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractPackData(String prompt) {
        try {
            return callGoogleAIWithRetry(buildExtractionPrompt("PACK", prompt));
        } catch (Exception e) {
            logger.error("❌ Erreur extraction pack IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractPackConfigurationData(String prompt) {
        try {
            return callGoogleAIWithRetry(buildExtractionPrompt("CONFIGURATION_PACK", prompt));
        } catch (Exception e) {
            logger.error("❌ Erreur extraction configuration pack IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> extractAddGarantieToPackData(String prompt) {
        try {
            return callGoogleAIWithRetry(buildExtractionPrompt("AJOUT_GARANTIE_PACK", prompt));
        } catch (Exception e) {
            logger.error("❌ Erreur extraction ajout garantie pack IA: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // ========== APPEL GEMINI AVEC RETRY ROBUSTE ==========

    private Map<String, Object> callGoogleAIWithRetry(String fullPrompt) throws IOException, InterruptedException {
        if (!isAIAvailable()) {
            logger.warn("⚠️ API KEY non configurée, fallback patterns");
            return new HashMap<>();
        }

        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                logger.debug("🔄 Tentative {}/{} Gemini", attempt + 1, maxRetries);
                Map<String, Object> result = callGoogleAI(fullPrompt);
                if (!result.isEmpty()) {
                    logger.info("✅ Extraction IA réussie!");
                    return result;
                }
            } catch (Exception e) {
                logger.warn("⚠️ Tentative {} échouée: {}", attempt + 1, e.getMessage());
                attempt++;
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs);
                }
            }
        }

        if (fallbackOnError) {
            logger.warn("❌ Fallback patterns activé");
            return new HashMap<>();
        }
        return new HashMap<>();
    }

    private Map<String, Object> callGoogleAI(String fullPrompt) throws IOException, InterruptedException {
        String url = String.format("%s/%s:generateContent?key=%s", geminiUrl, geminiModel, googleApiKey);
        logger.debug("📡 Appel Gemini: {}", geminiModel);

        String requestBody = String.format("""
            {
              "systemInstruction": {
                "parts": [
                  { "text": "%s" }
                ]
              },
              "contents": [
                {
                  "role": "user",
                  "parts": [
                    { "text": "%s" }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.1,
                "maxOutputTokens": 2048,
                "responseMimeType": "application/json"
              }
            }
            """, escapeJsonString(buildSystemInstruction()), escapeJsonString(fullPrompt));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("❌ Gemini error {}: {}", response.statusCode(), response.body());
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

    /**
     * Parse la réponse Gemini V1Beta.
     * IMPORTANT: on ne doit PAS extraire un JSON "au hasard" dans le body, sinon on parse la réponse API elle-même.
     * On lit d'abord le texte généré (candidates[0].content.parts[0].text), puis on parse ce texte en JSON.
     */
    private Map<String, Object> parseAIResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            String modelText = textNode.isMissingNode() ? "" : textNode.asText("");
            modelText = stripCodeFences(modelText).trim();

            if (modelText.isBlank()) {
                logger.warn("⚠️ Réponse Gemini vide (aucun texte)");
                return new HashMap<>();
            }

            // Nettoyage léger
            modelText = modelText.replaceAll("[\\x00-\\x1F]", "");

            Map<String, Object> parsed = objectMapper.readValue(modelText, new TypeReference<Map<String, Object>>() {});
            logger.debug("✅ JSON extracted from Gemini");
            return parsed;
        } catch (Exception e) {
            logger.warn("⚠️ Parse error Gemini: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private static String stripCodeFences(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replaceAll("^```(?:json)?\\s*", "")
            .replaceAll("\\s*```$", "");
    }

    private String buildSystemInstruction() {
        return """
            Tu es un moteur d'extraction d'informations pour un domaine d'assurance.
            Contraintes OBLIGATOIRES :
            - Réponds UNIQUEMENT par un JSON valide (pas de texte, pas de markdown).
            - N'invente pas de valeurs : mets null si l'information n'est pas présente.
            - Les montants sont des nombres (ex: 20000). Les durées sont en mois.
            - Les taux sont entre 0 et 1 (ex: 0.5 pour 50%).
            - Utilise ces enums quand applicable :
              * statut: ACTIF | INACTIF | EN_ATTENTE
              * typeProduit: SANTE | AUTO | HABITATION | VIE | PREVOYANCE | EPARGNE
              * niveauCouverture: BASIC | STANDARD | PREMIUM | GOLD
              * couvertureGeographique: NATIONAL | LOCAL | UE | MAGHREB | INTERNATIONAL
              * typeClients: tableau de INDIVIDUEL | FAMILLE | ENTREPRISE | SENIOR
              * typeGarantie: HOSPITALISATION | DENTAIRE | OPTIQUE | CONSULTATION | EXAMEN | MEDICAMENTS | SOINS_GENERAUX | INTERNATIONAL
              * typeMontant: FRAIS_REELS | FORFAIT | TARIF_CONVENTIONNE
            """;
    }

    private String buildExtractionPrompt(String kind, String userPrompt) {
        return switch (kind) {
            case "GARANTIE" -> """
                Extrais les informations d'une garantie d'assurance depuis le prompt utilisateur.

                JSON attendu (toutes les clés présentes) :
                {
                  "nom": null,
                  "description": null,
                  "type": null,
                  "tauxRemboursement": null,
                  "typeMontant": null,
                  "plafondAnnuel": null,
                  "plafondMensuel": null,
                  "plafondParActe": null,
                  "franchise": null,
                  "coutMoyenParSinistre": null,
                  "dureeMinContrat": null,
                  "dureeMaxContrat": null,
                  "resiliableAnnuellement": null,
                  "statut": null
                }

                Prompt utilisateur :
                """ + userPrompt;
            case "PRODUIT" -> """
                Extrais les informations d'un produit d'assurance depuis le prompt utilisateur.

                JSON attendu :
                {
                  "nom": null,
                  "description": null,
                  "typeProduit": null,
                  "statut": null
                }

                Prompt utilisateur :
                """ + userPrompt;
            case "PACK" -> """
                Extrais les informations d'un pack d'assurance depuis le prompt utilisateur.

                JSON attendu :
                {
                  "nom": null,
                  "description": null,
                  "ageMinimum": null,
                  "ageMaximum": null,
                  "typeClients": null,
                  "couvertureGeographique": null,
                  "prixMensuel": null,
                  "dureeMinContrat": null,
                  "dureeMaxContrat": null,
                  "niveauCouverture": null,
                  "statut": null,
                  "nomProduit": null,
                  "produitId": null
                }

                Prompt utilisateur :
                """ + userPrompt;
            case "CONFIGURATION_PACK" -> """
                Extrais les informations pour configurer un pack (liaison Pack <-> Garantie) depuis le prompt utilisateur.

                JSON attendu :
                {
                  "packId": null,
                  "garantieId": null,
                  "nomPack": null,
                  "nomGarantie": null,
                  "tauxRemboursement": null,
                  "plafond": null,
                  "franchise": null,
                  "optionnelle": null,
                  "supplementPrix": null,
                  "delaiCarence": null,
                  "priorite": null,
                  "condition": null,
                  "ordreAffichage": null
                }

                Prompt utilisateur :
                """ + userPrompt;
            case "AJOUT_GARANTIE_PACK" -> """
                Extrais les informations pour ajouter une garantie à un pack depuis le prompt utilisateur.

                JSON attendu :
                {
                  "nomPack": null,
                  "nomGarantie": null,
                  "tauxRemboursement": null,
                  "plafond": null,
                  "franchise": null,
                  "optionnelle": null,
                  "supplementPrix": null,
                  "delaiCarence": null,
                  "priorite": null
                }

                Prompt utilisateur :
                """ + userPrompt;
            case "RECOMMENDATION" -> """
                Analyse le besoin client et retourne un JSON.

                JSON attendu :
                {
                  "typeProduit": null,
                  "budgetMensuel": null,
                  "age": null,
                  "typeClient": null,
                  "niveauCouverture": null,
                  "couvertureGeographique": null,
                  "garantiesRecherchees": null,
                  "familyStatus": null,
                  "children": null
                }

                Prompt utilisateur :
                """ + userPrompt;
            default -> userPrompt;
        };
    }

    public Map<String, Object> extractRecommendationData(String prompt) {
        try {
            return callGoogleAIWithRetry(buildExtractionPrompt("RECOMMENDATION", prompt));
        } catch (Exception e) {
            logger.error("❌ Erreur extraction recommandation IA: {}", e.getMessage());
            return new HashMap<>();
        }
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
        
        // Strip leading French articles from nom field to fix extraction bug
        if ("nom".equals(key) && s != null) {
            s = s.replaceFirst("^(une?|la|le|les|des|du|de la|de l'|l')\\s+", "").trim();
        }
        
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
}

package tn.vermeg.gestionproduit.services.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.ChatbotResponseDTO;
import tn.vermeg.gestionproduit.dto.ChatbotGarantieRequestDTO;
import tn.vermeg.gestionproduit.dto.ChatbotProduitRequestDTO;
import tn.vermeg.gestionproduit.dto.ChatbotPackRequestDTO;
import tn.vermeg.gestionproduit.dto.SegmentedPromptDTO;
import tn.vermeg.gestionproduit.entities.*;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.services.GarantieService;
import tn.vermeg.gestionproduit.services.ProduitService;
import tn.vermeg.gestionproduit.services.PackUnifiedService;
import tn.vermeg.gestionproduit.services.ScoringService;

import java.util.*;
import java.util.Set;
import java.util.LinkedHashMap;
@Service
public class ChatbotOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotOrchestratorService.class);

    // Services chatbot
    private final PromptAnalyzerService promptAnalyzerService;
    private final PromptSegmentationService promptSegmentationService;
    private final AIExtractionService aiExtractionService;
    private final ActionNormalizationService normalizationService;
    private final ValidationService validationService;

    // Services métier existants (réutilisés)
    private final GarantieService garantieService;
    private final ProduitService produitService;
    private final PackUnifiedService packUnifiedService;

    // Repositories pour la recherche automatique
    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;
    private final ScoringService scoringService;

    public ChatbotOrchestratorService(
            PromptAnalyzerService promptAnalyzerService,
            PromptSegmentationService promptSegmentationService,
            AIExtractionService aiExtractionService,
            ActionNormalizationService normalizationService,
            ValidationService validationService,
            GarantieService garantieService,
            ProduitService produitService,
            PackUnifiedService packUnifiedService,
            ProduitRepository produitRepository,
            PackUnifiedRepository packUnifiedRepository,
            GarantieRepository garantieRepository,
            ScoringService scoringService) {
        this.promptAnalyzerService = promptAnalyzerService;
        this.promptSegmentationService = promptSegmentationService;
        this.aiExtractionService = aiExtractionService;
        this.normalizationService = normalizationService;
        this.validationService = validationService;
        this.garantieService = garantieService;
        this.produitService = produitService;
        this.packUnifiedService = packUnifiedService;
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
        this.scoringService = scoringService;
    }

     //Point d'entrée principal pour le traitement des prompts
    public ChatbotResponseDTO processPrompt(String prompt) {
        logger.info("Début du traitement du prompt: {}", prompt);

        try {
            // Étape 1: Validation du prompt
            ValidationService.ValidationResult promptValidation = validationService.validatePrompt(prompt);
            if (!promptValidation.isValid()) {
                return createErrorResponse("Prompt invalide", promptValidation.getErrors());
            }

            // Étape 2: Analyse et détection de l'action
            ChatbotAction action = promptAnalyzerService.analyzeAction(prompt);
            if (action == null) {
                return createErrorResponse("Action non détectée", 
                    List.of("Impossible de déterminer l'action à partir du prompt"));
            }

            // Étape 3: Validation de l'action supportée
            if (!validationService.isSupportedAction(action)) {
                return createErrorResponse("Action non supportée", 
                    List.of("L'action '" + action + "' n'est pas encore supportée"));
            }

            // Étape 4: Exécution de l'action
            Object result = executeAction(action, prompt);
            if (result instanceof Map<?, ?> resultMap && resultMap.containsKey("success") && Boolean.FALSE.equals(resultMap.get("success"))) {
                return createErrorResponse("Erreur lors de l'exécution", List.of(resultMap.get("error") != null ? resultMap.get("error").toString() : "Erreur inconnue"));
            }

            // Étape 5: Création de la réponse
            return createSuccessResponse(action, result, prompt, promptValidation.getWarnings());

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du prompt: {}", e.getMessage(), e);
            return createErrorResponse("Erreur interne", List.of("Une erreur est survenue: " + e.getMessage()));
        }
    }

     // Exécute l'action détectée
    private Object executeAction(ChatbotAction action, String prompt) {
        logger.info("Exécution de l'action: {}", action);

        return switch (action) {
            case GARANTIE -> executeCreateGarantie(prompt);
            case PRODUIT -> executeCreateProduit(prompt);
            case PACK -> executeCreatePack(prompt);
            case CONFIGURATION_PACK -> executeConfigurePack(prompt);
            case AJOUT_GARANTIE_PACK -> executeAddGarantieToPack(prompt);
            case RECOMMENDATION -> executeRecommendation(prompt);
            default -> Map.of("success", false, "error", "Action non implémentée: " + action);
        };
    }

     // Exécute la création d'une garantie
    private Object executeCreateGarantie(String prompt) {
        try {
            SegmentedPromptDTO segmented = promptSegmentationService.segment(prompt);
            String garantiesText = segmented.getGarantiesSection().isBlank() ? prompt : segmented.getGarantiesSection();

            Map<String, Object> extractedData;
            List<String> extractionWarnings = new ArrayList<>();
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractGarantieData(prompt);
                if (extractedData == null || extractedData.isEmpty()) {
                    extractionWarnings.add("Extraction IA vide/échouée → fallback regex appliqué.");
                    extractedData = extractGarantieDataFallback(garantiesText);
                }
            } else {
                extractedData = extractGarantieDataFallback(garantiesText);
                extractionWarnings.add("IA indisponible → extraction regex appliquée.");
            }

            ChatbotGarantieRequestDTO dto = createGarantieDTO(extractedData, prompt, garantiesText);
            dto.getWarnings().addAll(extractionWarnings);

            // Normalisation
            Garantie garantie = normalizeAndConvertToGarantie(dto);

            // Validation
            ValidationService.ValidationResult validation = validationService.validateGarantie(garantie);
            if (!validation.isValid()) {
                return Map.of(
                    "success", false,
                    "error", "Validation échouée",
                    "details", validation.getErrors(),
                    "warnings", validation.getWarnings()
                );
            }

            // Application des valeurs par défaut
            normalizationService.applyGarantieDefaults(garantie);

            // Création via le service métier existant
            Garantie created;
            try {
                created = garantieService.createGarantie(garantie);
            } catch (IllegalArgumentException e) {
                if (e.getMessage() != null && e.getMessage().contains("existe déjà")) {
                    return Map.of(
                        "success", false,
                        "error", "Garantie déjà existante",
                        "details", List.of(e.getMessage()),
                        "suggestion", "Vous pouvez soit modifier le nom de la garantie, soit mettre à jour la garantie existante."
                    );
                }
                throw e;
            }

            return Map.of(
                "success", true,
                "action", "CREATE_GARANTIE",
                "entity", created,
                "message", "Garantie créée avec succès",
                "id", created.getIdGarantie(),
                "warnings", validation.getWarnings()
            );

        } catch (Exception e) {
            logger.error("Erreur création garantie: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Erreur création garantie: " + e.getMessage());
        }
    }

     // Exécute la création d'un produit
    private Object executeCreateProduit(String prompt) {
        try {
            // Extraction des données
            Map<String, Object> extractedData;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractProduitData(prompt);
            } else {
                extractedData = extractProduitDataFallback(prompt);
            }

            // Création du dto
            ChatbotProduitRequestDTO dto = createProduitDTO(extractedData, prompt);

            // Normalisation
            Produit produit = normalizeAndConvertToProduit(dto);

            // Validation
            ValidationService.ValidationResult validation = validationService.validateProduit(produit);
            if (!validation.isValid()) {
                return Map.of(
                    "success", false,
                    "error", "Validation échouée",
                    "details", validation.getErrors(),
                    "warnings", validation.getWarnings()
                );
            }

            // Application des valeurs par défaut
            normalizationService.applyProduitDefaults(produit);

            // Création via le service métier existant
            Produit created = produitService.createProduit(produit);

            return Map.of(
                "success", true,
                "action", "CREATE_PRODUIT",
                "entity", created,
                "message", "Produit créé avec succès",
                "id", created.getIdProduit(),
                "warnings", validation.getWarnings()
            );

        } catch (Exception e) {
            logger.error("Erreur création produit: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Erreur création produit: " + e.getMessage());
        }
    }

     //Exécute la création d'un pack
    private Object executeCreatePack(String prompt) {
        try {
            SegmentedPromptDTO segmented = promptSegmentationService.segment(prompt);
            String packText = segmented.getPackSection().isBlank() ? prompt : segmented.getPackSection();

            Map<String, Object> extractedData;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractPackData(prompt);
                mergePackRegexFallback(extractedData, packText);
            } else {
                extractedData = extractPackDataFallback(packText);
            }

            ChatbotPackRequestDTO dto = createPackDTO(extractedData, prompt, packText);

            linkPackToExistingProduit(dto, packText);

            Pack pack = normalizeAndConvertToPack(dto);

            // Validation
            ValidationService.ValidationResult validation = validationService.validatePack(pack);
            if (!validation.isValid()) {
                return Map.of(
                    "success", false,
                    "error", "Validation échouée",
                    "details", validation.getErrors(),
                    "warnings", validation.getWarnings()
                );
            }

            // Application des valeurs par défaut
            normalizationService.applyPackDefaults(pack);

            // Création via le service métier existant
            Pack created = packUnifiedService.createPack(pack);

            return Map.of(
                "success", true,
                "action", "CREATE_PACK",
                "entity", created,
                "message", "Pack créé avec succès",
                "id", created.getIdPack(),
                "warnings", validation.getWarnings()
            );

        } catch (Exception e) {
            logger.error("Erreur création pack: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Erreur création pack: " + e.getMessage());
        }
    }
     //Exécute la configuration d'un pack
    private Object executeConfigurePack(String prompt) {
        try {
            // Extraction des données
            Map<String, Object> extractedData;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractPackConfigurationData(prompt);
            } else {
                extractedData = extractPackConfigurationDataFallback(prompt);
            }

            // Création de l'entité PackGarantie
            PackGarantie packGarantie = createPackGarantieFromData(extractedData);

            // Validation
            ValidationService.ValidationResult validation = validationService.validatePackConfiguration(packGarantie);
            if (!validation.isValid()) {
                return Map.of(
                    "success", false,
                    "error", "Validation échouée",
                    "details", validation.getErrors(),
                    "warnings", validation.getWarnings()
                );
            }

            // Recherche des IDs
            String packId = findPackId(extractedData);
            String garantieId = findGarantieId(extractedData);

            if (packId == null || garantieId == null) {
                return Map.of(
                    "success", false,
                    "error", "Entités non trouvées",
                    "details", List.of("Pack ou garantie non trouvé(e) avec les informations fournies")
                );
            }

            // Configuration via le service métier existant
            PackGarantie created = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);

            return Map.of(
                "success", true,
                "action", "CONFIGURE_PACK",
                "entity", created,
                "message", "Pack configuré avec succès",
                "id", created.getIdPackGarantie()
            );

        } catch (Exception e) {
            logger.error("Erreur configuration pack: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Erreur configuration pack: " + e.getMessage());
        }
    }

     // Exécute l'ajout de garantie à un pack
    private Object executeAddGarantieToPack(String prompt) {
        try {
            // Extraction des données
            Map<String, Object> extractedData;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractAddGarantieToPackData(prompt);
            } else {
                extractedData = extractAddGarantieToPackDataFallback(prompt);
            }

            // Création de l'entité PackGarantie
            PackGarantie packGarantie = createPackGarantieFromData(extractedData);

            // Validation
            ValidationService.ValidationResult validation = validationService.validatePackConfiguration(packGarantie);
            if (!validation.isValid()) {
                return Map.of(
                    "success", false,
                    "error", "Validation échouée",
                    "details", validation.getErrors(),
                    "warnings", validation.getWarnings()
                );
            }

            // Recherche des IDs
            String packId = findPackId(extractedData);
            String garantieId = findGarantieId(extractedData);

            if (packId == null || garantieId == null) {
                return Map.of(
                    "success", false,
                    "error", "Entités non trouvées",
                    "details", List.of("Pack ou garantie non trouvé(e) avec les informations fournies")
                );
            }

            // Ajout via le service métier existant
            PackGarantie created = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);

            return Map.of(
                "success", true,
                "action", "ADD_GARANTIE_TO_PACK",
                "entity", created,
                "message", "Garantie ajoutée au pack avec succès",
                "id", created.getIdPackGarantie()
            );

        } catch (Exception e) {
            logger.error("Erreur ajout garantie au pack: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Erreur ajout garantie au pack: " + e.getMessage());
        }
    }

    // Exécute une recommandation personnalisée basée sur les besoins utilisateur
    @SuppressWarnings("unchecked")
    private Object executeRecommendation(String prompt) {
        try {
            Map<String, Object> extractedData = Map.of();
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractRecommendationData(prompt);
            }

            String requestedProductType = aiExtractionService.getStringValue(extractedData, "typeProduit", "").toUpperCase(Locale.ROOT);
            int age = aiExtractionService.getIntegerValue(extractedData, "age", 0);
            double budget = aiExtractionService.getDoubleValue(extractedData, "budgetMensuel", 0);
            String typeClient = aiExtractionService.getStringValue(extractedData, "typeClient", "");
            String niveauCouverture = aiExtractionService.getStringValue(extractedData, "niveauCouverture", "");
            String couvertureGeographique = aiExtractionService.getStringValue(extractedData, "couvertureGeographique", "");
            List<String> garantiesRecherchees = extractStringList(extractedData, "garantiesRecherchees");
            List<String> maladiesChroniques = extractStringList(extractedData, "maladiesChroniques");
            int nombreBeneficiaires = aiExtractionService.getIntegerValue(extractedData, "nombreBeneficiaires", 1);
            String situationFamiliale = aiExtractionService.getStringValue(extractedData, "situationFamiliale", "");
            if (garantiesRecherchees.isEmpty()) {
                garantiesRecherchees = extractStringList(extractedData, "garantiesSouhaitees");
            }

            if (age == 0) {
                age = promptAnalyzerService.extractAgeMinimum(prompt);
            }
            if (budget <= 0) {
                Double extractedBudget = promptAnalyzerService.extractPrixMensuel(prompt);
                budget = extractedBudget != null ? extractedBudget : budget;
            }
            if (couvertureGeographique.isBlank()) {
                couvertureGeographique = promptAnalyzerService.extractCouvertureGeographique(prompt);
            }
            if (typeClient.isBlank()) {
                List<String> detectedTypes = promptAnalyzerService.extractTypeClientLabels(prompt);
                typeClient = detectedTypes.isEmpty() ? "" : detectedTypes.get(0);
            }
            if (requestedProductType.isBlank()) {
                requestedProductType = extractProductTypeFromPrompt(prompt);
            }
            if (niveauCouverture.isBlank()) {
                niveauCouverture = promptAnalyzerService.extractNiveauCouverture(prompt);
            }

            ScoringService.ClientProfile profile = new ScoringService.ClientProfile(
                typeClient,
                age,
                nombreBeneficiaires,
                budget,
                garantiesRecherchees,
                couvertureGeographique,
                requestedProductType,
                maladiesChroniques,
                situationFamiliale
            );

            List<ScoringService.RecommendationDTO> rankedPacks = scoringService.recommendPacks(profile);
            List<ScoringService.RecommendationDTO> rankedProduits = scoringService.recommendProduits(profile);

            List<Map<String, Object>> topPacks = new ArrayList<>();
            for (int i = 0; i < Math.min(3, rankedPacks.size()); i++) {
                ScoringService.RecommendationDTO packRec = rankedPacks.get(i);
                topPacks.add(Map.of(
                    "packId", packRec.packId(),
                    "nomPack", packRec.nomPack(),
                    "score", packRec.score(),
                    "reason", packRec.reason()
                ));
            }

            List<Map<String, Object>> topProduits = new ArrayList<>();
            for (int i = 0; i < Math.min(2, rankedProduits.size()); i++) {
                ScoringService.RecommendationDTO prodRec = rankedProduits.get(i);
                topProduits.add(Map.of(
                    "produitId", prodRec.packId(),
                    "nomProduit", prodRec.nomPack(),
                    "score", prodRec.score(),
                    "reason", prodRec.reason()
                ));
            }

            Map<String, Object> criteria = new LinkedHashMap<>();
            criteria.put("age", age);
            criteria.put("budgetMensuel", budget);
            if (!requestedProductType.isBlank()) criteria.put("typeProduit", requestedProductType);
            if (!typeClient.isBlank()) criteria.put("typeClient", typeClient);
            if (!niveauCouverture.isBlank()) criteria.put("niveauCouverture", niveauCouverture);
            if (!couvertureGeographique.isBlank()) criteria.put("couvertureGeographique", couvertureGeographique);
            if (!garantiesRecherchees.isEmpty()) criteria.put("garantiesRecherchees", garantiesRecherchees);

            String generatedPrompt = buildGeminiRecommendationPrompt(prompt, profile, topPacks, topProduits);

            return Map.of(
                "success", true,
                "action", "RECOMMENDATION",
                "topPacks", topPacks,
                "topProduits", topProduits,
                "criteria", criteria,
                "generatedPrompt", generatedPrompt,
                "message", (!topPacks.isEmpty() || !topProduits.isEmpty())
                    ? "Recommandations calculées avec succès."
                    : "Aucune recommandation disponible pour les critères fournis."
            );

        } catch (Exception e) {
            logger.error("Erreur recommandation: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Erreur recommandation: " + e.getMessage());
        }
    }

    private List<String> extractStringList(Map<String, Object> data, String key) {
        if (data == null || !data.containsKey(key)) {
            return List.of();
        }
        Object rawValue = data.get(key);
        if (rawValue instanceof List<?> list) {
            List<String> values = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    String value = item.toString().trim();
                    if (!value.isEmpty()) {
                        values.add(value);
                    }
                }
            }
            return values;
        }
        String text = rawValue.toString().trim();
        if (text.isEmpty()) {
            return List.of();
        }
        text = text.replaceAll("[\\[\\]\\\"']", "");
        String[] parts = text.split("[,;\\n]");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String extractProductTypeFromPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        String lower = prompt.toLowerCase();
        if (lower.contains("santé") || lower.contains("sante")) return "SANTE";
        if (lower.contains("auto") || lower.contains("voiture")) return "AUTO";
        if (lower.contains("habitation") || lower.contains("maison")) return "HABITATION";
        if (lower.contains("vie") || lower.contains("décès")) return "VIE";
        if (lower.contains("épargne") || lower.contains("epargne")) return "EPARGNE";
        if (lower.contains("prévoyance") || lower.contains("prevoyance")) return "PREVOYANCE";
        return "";
    }

    private String buildGeminiRecommendationPrompt(String originalPrompt,
                                                   ScoringService.ClientProfile profile,
                                                   List<Map<String, Object>> topPacks,
                                                   List<Map<String, Object>> topProduits) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Vous êtes un assistant de recommandation d'assurance.");
        promptBuilder.append("\n\nDemande client : ").append(originalPrompt).append("\n\n");
        promptBuilder.append("Critères extraits :\n");
        promptBuilder.append("- Age : ").append(profile.age()).append(" ans\n");
        promptBuilder.append("- Budget mensuel maximal : ").append(profile.budget()).append("\n");
        if (!profile.produitTypePreferee().isBlank()) {
            promptBuilder.append("- Type de produit préféré : ").append(profile.produitTypePreferee()).append("\n");
        }
        if (!profile.couvertureGeographique().isBlank()) {
            promptBuilder.append("- Couverture géographique : ").append(profile.couvertureGeographique()).append("\n");
        }
        if (!profile.garantiesVoulu().isEmpty()) {
            promptBuilder.append("- Garanties recherchées : ").append(String.join(", ", profile.garantiesVoulu())).append("\n");
        }
        if (!profile.sexe().isBlank()) {
            promptBuilder.append("- Type de client : ").append(profile.sexe()).append("\n");
        }
        promptBuilder.append("\nTop recommandations calculées :\n");
        if (!topPacks.isEmpty()) {
            promptBuilder.append("Packs :\n");
            for (Map<String, Object> pack : topPacks) {
                promptBuilder.append("  - ")
                    .append(pack.get("nomPack"))
                    .append(" (score ")
                    .append(pack.get("score"))
                    .append(") : ")
                    .append(pack.get("reason"))
                    .append("\n");
            }
        }
        if (!topProduits.isEmpty()) {
            promptBuilder.append("Produits :\n");
            for (Map<String, Object> produit : topProduits) {
                promptBuilder.append("  - ")
                    .append(produit.get("nomProduit"))
                    .append(" (score ")
                    .append(produit.get("score"))
                    .append(") : ")
                    .append(produit.get("reason"))
                    .append("\n");
            }
        }
        promptBuilder.append("\nTâche :");
        promptBuilder.append("\n1. Analysez les besoins du client.");
        promptBuilder.append("\n2. Vérifiez la cohérence des recommandations.");
        promptBuilder.append("\n3. Proposez une réponse structurée et facile à comprendre.");
        promptBuilder.append("\n4. Incluez une explication des critères de sélection.");
        promptBuilder.append("\nRetournez uniquement le texte de la recommandation, sans métadonnées internes.");
        return promptBuilder.toString();
    }

    // Méthodes utilitaires de conversion et normalisation
    private ChatbotGarantieRequestDTO createGarantieDTO(Map<String, Object> extractedData, String prompt, String analysisText) {
        ChatbotGarantieRequestDTO dto = new ChatbotGarantieRequestDTO();
        dto.setOriginalPrompt(prompt);

        // --- Nom: ne garder la valeur IA que si elle est cohérente avec le prompt (sinon fallback regex) ---
        String regexNom = promptAnalyzerService.extractNomGarantie(analysisText);
        String aiNom = aiExtractionService.getStringValue(extractedData, "nom", null);
        if (aiNom != null && !aiNom.isBlank()) {
            String aiNomClean = aiNom.trim();
            boolean appearsInPrompt = analysisText != null
                    && analysisText.toLowerCase(Locale.ROOT).contains(aiNomClean.toLowerCase(Locale.ROOT));
            boolean looksValid = aiNomClean.length() >= 3 && aiNomClean.matches("^[\\p{L}0-9 _'\\-]{3,80}$");
            dto.setNomGarantie((appearsInPrompt || looksValid) ? aiNomClean : regexNom);
            if (!appearsInPrompt && regexNom != null && !regexNom.equalsIgnoreCase(aiNomClean)) {
                dto.addWarning("Nom IA incohérent détecté ('" + aiNomClean + "') → nom regex utilisé ('" + regexNom + "').");
            }
        } else {
            dto.setNomGarantie(regexNom);
        }

        dto.setDescription(aiExtractionService.getStringValue(extractedData, "description", promptAnalyzerService.extractDescription(analysisText)));
        dto.setType(normalizationService.normalizeTypeGarantie(
            aiExtractionService.getStringValue(extractedData, "type", promptAnalyzerService.extractTypeGarantie(analysisText))));
        dto.setTauxRemboursement(aiExtractionService.getDoubleValue(extractedData, "tauxRemboursement", promptAnalyzerService.extractTauxRemboursement(analysisText)));
        dto.setTypeMontant(aiExtractionService.getStringValue(extractedData, "typeMontant", promptAnalyzerService.extractTypeMontant(analysisText)));
        
        PromptAnalyzerService.PlafondData plafonds = promptAnalyzerService.extractPlafonds(analysisText);
        dto.setPlafondAnnuel(aiExtractionService.getDoubleValue(extractedData, "plafondAnnuel", plafonds.annuel));
        dto.setPlafondMensuel(aiExtractionService.getDoubleValue(extractedData, "plafondMensuel", plafonds.mensuel));
        dto.setPlafondParActe(aiExtractionService.getDoubleValue(extractedData, "plafondParActe", plafonds.parActe));
        
        dto.setFranchise(aiExtractionService.getDoubleValue(
            extractedData,
            "franchise",
            Optional.ofNullable(promptAnalyzerService.extractFranchise(analysisText)).orElse(0.0)
        ));
        dto.setCoutMoyenParSinistre(aiExtractionService.getDoubleValue(extractedData, "coutMoyenParSinistre", promptAnalyzerService.extractCoutMoyenParSinistre(analysisText)));
        dto.setDureeMinContrat(aiExtractionService.getIntegerValue(extractedData, "dureeMinContrat", promptAnalyzerService.extractDureeMinContrat(analysisText)));
        dto.setDureeMaxContrat(aiExtractionService.getIntegerValue(extractedData, "dureeMaxContrat", promptAnalyzerService.extractDureeMaxContrat(analysisText)));
        
        Boolean resiliable = promptAnalyzerService.extractResiliableAnnuellement(analysisText);
        dto.setResiliableAnnuellement(aiExtractionService.getBooleanValue(extractedData, "resiliableAnnuellement", resiliable != null ? resiliable : false));
        
        Statut statutValue = normalizationService.normalizeStatut(aiExtractionService.getStringValue(extractedData, "statut", null));
        dto.setStatut(statutValue != null ? statutValue.toString() : null);
        dto.generateBusinessHash();
        return dto;
    }

    private Garantie normalizeAndConvertToGarantie(ChatbotGarantieRequestDTO dto) {
        Garantie garantie = new Garantie();
        garantie.setNomGarantie(dto.getNomGarantie());
        garantie.setDescription(dto.getDescription());
        garantie.setType(dto.getType());
        garantie.setTauxRemboursement(dto.getTauxRemboursement());
        garantie.setTypeMontant(normalizationService.normalizeTypeMontant(dto.getTypeMontant()));
        garantie.setPlafondAnnuel(dto.getPlafondAnnuel());
        garantie.setPlafondMensuel(dto.getPlafondMensuel());
        garantie.setPlafondParActe(dto.getPlafondParActe());
        garantie.setFranchise(dto.getFranchise());
        garantie.setCoutMoyenParSinistre(dto.getCoutMoyenParSinistre());
        garantie.setDureeMinContrat(dto.getDureeMinContrat());
        garantie.setDureeMaxContrat(dto.getDureeMaxContrat());
        garantie.setResiliableAnnuellement(dto.getResiliableAnnuellement());
        garantie.setTypePlafond(TypePlafond.ANNUEL);
        garantie.setStatut(normalizationService.normalizeStatut(dto.getStatut()));
        garantie.setCreePar("AI-Chatbot");
        return garantie;
    }

    private ChatbotProduitRequestDTO createProduitDTO(Map<String, Object> extractedData, String prompt) {
        ChatbotProduitRequestDTO dto = new ChatbotProduitRequestDTO();
        dto.setOriginalPrompt(prompt);
        dto.setNomProduit(aiExtractionService.getStringValue(extractedData, "nom", promptAnalyzerService.extractNomProduit(prompt)));
        dto.setDescription(aiExtractionService.getStringValue(extractedData, "description", promptAnalyzerService.extractDescription(prompt)));
        TypeProduit typeProduitValue = normalizationService.normalizeTypeProduit(
            aiExtractionService.getStringValue(extractedData, "typeProduit", promptAnalyzerService.extractTypeProduit(prompt)));
        dto.setTypeProduit(typeProduitValue != null ? typeProduitValue.toString() : null);
        Statut statutValue2 = normalizationService.normalizeStatut(aiExtractionService.getStringValue(extractedData, "statut", null));
        dto.setStatut(statutValue2 != null ? statutValue2.toString() : null);
        dto.generateBusinessHash();
        return dto;
    }

    private Produit normalizeAndConvertToProduit(ChatbotProduitRequestDTO dto) {
        Produit produit = new Produit();
        produit.setNomProduit(dto.getNomProduit());
        produit.setDescription(dto.getDescription());
        produit.setTypeProduit(normalizationService.normalizeTypeProduit(dto.getTypeProduit()));
        produit.setStatut(normalizationService.normalizeStatut(dto.getStatut()));
        return produit;
    }

    private ChatbotPackRequestDTO createPackDTO(Map<String, Object> extractedData, String prompt, String packText) {
        ChatbotPackRequestDTO dto = new ChatbotPackRequestDTO();
        dto.setOriginalPrompt(prompt);
        dto.setNomPack(aiExtractionService.getStringValue(extractedData, "nom", promptAnalyzerService.extractNomPack(packText)));
        dto.setDescription(aiExtractionService.getStringValue(extractedData, "description", promptAnalyzerService.extractDescription(packText)));
        dto.setAgeMinimum(aiExtractionService.getIntegerValue(extractedData, "ageMinimum", promptAnalyzerService.extractAgeMinimum(packText)));
        dto.setAgeMaximum(aiExtractionService.getIntegerValue(extractedData, "ageMaximum", promptAnalyzerService.extractAgeMaximum(packText)));
        dto.setAncienneteContratMois(aiExtractionService.getIntegerValue(extractedData, "ancienneteContratMois", promptAnalyzerService.extractAncienneteContrat(packText)));
        CouvertureGeographique couvertureValue = normalizationService.normalizeCouvertureGeographique(
            aiExtractionService.getStringValue(extractedData, "couvertureGeographique", promptAnalyzerService.extractCouvertureGeographique(packText)));
        dto.setCouvertureGeographique(couvertureValue != null ? couvertureValue.toString() : null);
        dto.setPrixMensuel(aiExtractionService.getDoubleValue(extractedData, "prixMensuel", promptAnalyzerService.extractPrixMensuel(packText)));
        dto.setDureeMinContrat(aiExtractionService.getIntegerValue(extractedData, "dureeMinContrat", promptAnalyzerService.extractDureeMinContrat(packText)));
        dto.setDureeMaxContrat(aiExtractionService.getIntegerValue(extractedData, "dureeMaxContrat", promptAnalyzerService.extractDureeMaxContrat(packText)));
        NiveauCouverture niveauValue = normalizationService.normalizeNiveauCouverture(
            aiExtractionService.getStringValue(extractedData, "niveauCouverture", promptAnalyzerService.extractNiveauCouverture(packText)));
        dto.setNiveauCouverture(niveauValue != null ? niveauValue.toString() : null);
        Statut statutValue3 = normalizationService.normalizeStatut(aiExtractionService.getStringValue(extractedData, "statut", null));
        dto.setStatut(statutValue3 != null ? statutValue3.toString() : null);
        dto.setProduitId(aiExtractionService.getStringValue(extractedData, "produitId", null));
        dto.setNomProduit(aiExtractionService.getStringValue(extractedData, "nomProduit", null));

        List<String> fromAi = extractTypeClientStringsFromMap(extractedData);
        List<String> fromRegex = promptAnalyzerService.extractTypeClientLabels(packText);
        if (!fromAi.isEmpty()) {
            dto.setTypeClients(normalizationService.normalizeTypeClients(fromAi));
        } else if (!fromRegex.isEmpty()) {
            dto.setTypeClients(normalizationService.normalizeTypeClients(fromRegex));
        }

        dto.generateBusinessHash();
        return dto;
    }

    private Pack normalizeAndConvertToPack(ChatbotPackRequestDTO dto) {
        Pack pack = new Pack();
        pack.setNomPack(dto.getNomPack());
        pack.setDescription(dto.getDescription());
        pack.setAgeMinimum(dto.getAgeMinimum());
        pack.setAgeMaximum(dto.getAgeMaximum());
        pack.setAncienneteContratMois(dto.getAncienneteContratMois());
        pack.setCouvertureGeographique(normalizationService.normalizeCouvertureGeographique(dto.getCouvertureGeographique()));
        pack.setPrixMensuel(dto.getPrixMensuel() != null ? dto.getPrixMensuel() : 0.0);
        pack.setDureeMinContrat(dto.getDureeMinContrat() != null ? dto.getDureeMinContrat() : 0);
        pack.setDureeMaxContrat(dto.getDureeMaxContrat() != null ? dto.getDureeMaxContrat() : 0);
        pack.setNiveauCouverture(normalizationService.normalizeNiveauCouverture(dto.getNiveauCouverture()));
        pack.setStatut(normalizationService.normalizeStatut(dto.getStatut()));
        pack.setProduitId(dto.getProduitId());
        pack.setNomProduit(dto.getNomProduit());
        pack.setTypeClients(dto.getTypeClients());
        return pack;
    }

    private void linkPackToExistingProduit(ChatbotPackRequestDTO dto, String prompt) {
        String typeProduit = promptAnalyzerService.extractTypeProduit(prompt);
        if (typeProduit == null) {
            dto.addWarning("Type de produit non détecté pour liaison automatique");
            return;
        }

        TypeProduit normalizedType = normalizationService.normalizeTypeProduit(typeProduit);
        Optional<Produit> existingProduit = produitRepository.findByTypeProduit(normalizedType)
            .stream()
            .findFirst();

        if (existingProduit.isPresent()) {
            Produit produit = existingProduit.get();
            dto.setProduitId(produit.getIdProduit());
            dto.setNomProduit(produit.getNomProduit());
            dto.addWarning("Pack lié automatiquement au produit existant: " + produit.getNomProduit());
        } else {
            dto.addWarning("Type produit détecté (" + normalizedType + ") mais aucun produit existant trouvé");
        }
    }

    // Méthodes de création de réponses

    private ChatbotResponseDTO createSuccessResponse(ChatbotAction action, Object result, String prompt, List<String> warnings) {
        ChatbotResponseDTO response = new ChatbotResponseDTO();
        response.setSuccess(true);
        response.setAction(action.name());
        response.setResult(result);
        response.setPrompt(prompt);
        response.setWarnings(warnings);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    private ChatbotResponseDTO createErrorResponse(String message, List<String> errors) {
        ChatbotResponseDTO response = new ChatbotResponseDTO();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrors(errors);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    // Méthodes fallback (quand l'IA n'est pas disponible)

    private Map<String, Object> extractGarantieDataFallback(String prompt) {
        Map<String, Object> data = new HashMap<>();
        data.put("nom", promptAnalyzerService.extractNomGarantie(prompt));
        data.put("type", promptAnalyzerService.extractTypeGarantie(prompt));
        data.put("tauxRemboursement", promptAnalyzerService.extractTauxRemboursement(prompt));
        PromptAnalyzerService.PlafondData plafonds = promptAnalyzerService.extractPlafonds(prompt);
        data.put("plafondAnnuel", plafonds.annuel);
        data.put("plafondMensuel", plafonds.mensuel);
        data.put("plafondParActe", plafonds.parActe);
        data.put("franchise", promptAnalyzerService.extractFranchise(prompt));
        return data;
    }

    private Map<String, Object> extractProduitDataFallback(String prompt) {
        Map<String, Object> data = new HashMap<>();
        data.put("nom", promptAnalyzerService.extractNomProduit(prompt));
        data.put("typeProduit", promptAnalyzerService.extractTypeProduit(prompt));
        data.put("description", promptAnalyzerService.extractDescription(prompt));
        return data;
    }

    /**
     * Complète les champs vides ou incorrects de l'IA avec les regex sur la zone pack uniquement.
     */
    private void mergePackRegexFallback(Map<String, Object> data, String packText) {
        if (data == null || packText == null || packText.isBlank()) {
            return;
        }
        Map<String, Object> fb = extractPackDataFallback(packText);
        for (Map.Entry<String, Object> e : fb.entrySet()) {
            if (shouldMergePackField(e.getKey(), data.get(e.getKey()), e.getValue())) {
                data.put(e.getKey(), e.getValue());
            }
        }
    }

    private static boolean shouldMergePackField(String key, Object existing, Object fallback) {
        if (existing == null) {
            return true;
        }
        if (existing instanceof String s && s.isBlank()) {
            return true;
        }
        if ("nom".equals(key) && existing instanceof String s && s.length() < 3 && fallback instanceof String fs && fs.length() >= s.length()) {
            return true;
        }
        if ("prixMensuel".equals(key) && existing instanceof Number n && fallback instanceof Number fn) {
            return n.doubleValue() == 0.0 && fn.doubleValue() > 0.0;
        }
        return false;
    }

    private List<String> extractTypeClientStringsFromMap(Map<String, Object> data) {
        if (data == null) {
            return List.of();
        }
        Object tc = data.get("typeClients");
        if (tc instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    String s = o.toString().trim();
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
            return out;
        }
        if (tc instanceof String s && !s.isBlank()) {
            List<String> out = new ArrayList<>();
            for (String p : s.split("[,;|]+")) {
                if (p != null && !p.trim().isEmpty()) {
                    out.add(p.trim());
                }
            }
            return out;
        }
        Object single = data.get("typeClient");
        if (single != null) {
            String s = single.toString().trim();
            if (!s.isEmpty()) {
                return List.of(s);
            }
        }
        return List.of();
    }

    private Map<String, Object> extractPackDataFallback(String prompt) {
        Map<String, Object> data = new HashMap<>();
        data.put("nom", promptAnalyzerService.extractNomPack(prompt));
        data.put("ageMinimum", promptAnalyzerService.extractAgeMinimum(prompt));
        data.put("ageMaximum", promptAnalyzerService.extractAgeMaximum(prompt));
        data.put("prixMensuel", promptAnalyzerService.extractPrixMensuel(prompt));
        data.put("couvertureGeographique", promptAnalyzerService.extractCouvertureGeographique(prompt));
        data.put("niveauCouverture", promptAnalyzerService.extractNiveauCouverture(prompt));
        return data;
    }

    private Map<String, Object> extractPackConfigurationDataFallback(String prompt) {
        return new HashMap<>(); // Implémentation simplifiée
    }

    private Map<String, Object> extractAddGarantieToPackDataFallback(String prompt) {
        return new HashMap<>(); // Implémentation simplifiée
    }

    private PackGarantie createPackGarantieFromData(Map<String, Object> data) {
        PackGarantie packGarantie = new PackGarantie();
        packGarantie.setTauxRemboursement(aiExtractionService.getDoubleValue(data, "tauxRemboursement", 0.8));
        packGarantie.setPlafond(aiExtractionService.getDoubleValue(data, "plafond", 1000.0));
        packGarantie.setFranchise(aiExtractionService.getDoubleValue(data, "franchise", 0.0));
        packGarantie.setOptionnelle(aiExtractionService.getBooleanValue(data, "optionnelle", false));
        packGarantie.setSupplementPrix(aiExtractionService.getDoubleValue(data, "supplementPrix", 0.0));
        packGarantie.setDelaiCarence(aiExtractionService.getIntegerValue(data, "delaiCarence", 0));
        packGarantie.setPriorite(aiExtractionService.getIntegerValue(data, "priorite", 1));
        return packGarantie;
    }

    private String findPackId(Map<String, Object> data) {
        String packId = aiExtractionService.getStringValue(data, "packId", null);
        if (packId != null && !packId.isBlank()) {
            String trimmed = packId.trim();
            if (packUnifiedRepository.findById(trimmed).isPresent()) {
                return trimmed;
            }
            logger.warn("packId fourni mais introuvable en base: {}", trimmed);
        }

        String nomPack = aiExtractionService.getStringValue(data, "nomPack", null);
        if (nomPack != null && !nomPack.isBlank()) {
            return packUnifiedRepository.findFirstByNomPackIgnoreCase(nomPack.trim())
                    .map(Pack::getIdPack)
                    .orElse(null);
        }
        return null;
    }

    private String findGarantieId(Map<String, Object> data) {
        String garantieId = aiExtractionService.getStringValue(data, "garantieId", null);
        if (garantieId != null && !garantieId.isBlank()) {
            String trimmed = garantieId.trim();
            if (garantieRepository.findById(trimmed).isPresent()) {
                return trimmed;
            }
            logger.warn("garantieId fourni mais introuvable en base: {}", trimmed);
        }

        String nomGarantie = aiExtractionService.getStringValue(data, "nomGarantie", null);
        if (nomGarantie != null && !nomGarantie.isBlank()) {
            return garantieRepository.findFirstByNomGarantieIgnoreCase(nomGarantie.trim())
                    .map(Garantie::getIdGarantie)
                    .orElse(null);
        }
        return null;
    }
}

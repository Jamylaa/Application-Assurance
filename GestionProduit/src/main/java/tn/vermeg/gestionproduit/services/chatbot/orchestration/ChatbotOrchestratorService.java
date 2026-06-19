package tn.vermeg.gestionproduit.services.chatbot.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.ChatbotResponseDTO;
import tn.vermeg.gestionproduit.dto.ChatbotGarantieRequestDTO;
import tn.vermeg.gestionproduit.dto.ChatbotProduitRequestDTO;
import tn.vermeg.gestionproduit.dto.ChatbotPackRequestDTO;
import tn.vermeg.gestionproduit.dto.SegmentedPromptDTO;
import tn.vermeg.gestionproduit.dto.chatbot.PackCreationResponseDTO;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.CouvertureGeographique;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.*;
import tn.vermeg.gestionproduit.enums.TypeMontant;
import tn.vermeg.gestionproduit.enums.TypePlafond;
import tn.vermeg.gestionproduit.enums.TypeProduit;
import tn.vermeg.gestionproduit.services.chatbot.memory.ConversationMemoryService;
import tn.vermeg.gestionproduit.services.chatbot.rag.RAGService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.PromptAnalyzerService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.PromptSegmentationService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.AIExtractionService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.NLPNormalizationService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.ExtractionLoggingService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.EnhancedNumericExtractionService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.ComplexGuaranteeExtractionService;
import tn.vermeg.gestionproduit.services.chatbot.analysis.FuzzyProductMatcherService;
import tn.vermeg.gestionproduit.services.chatbot.scoring.RecommendationService;
import tn.vermeg.gestionproduit.services.chatbot.core.ChatbotAction;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.PackGarantieRepository;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.services.GarantieService;
import tn.vermeg.gestionproduit.services.ProduitService;
import tn.vermeg.gestionproduit.services.PackUnifiedService;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
 // Coordonne tous les services chatbot et appelle les services métier existants
@Service
public class ChatbotOrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotOrchestratorService.class);
    // Services chatbot
    private final PromptAnalyzerService promptAnalyzerService;
    private final PromptSegmentationService promptSegmentationService;
    private final AIExtractionService aiExtractionService;
    private final NLPNormalizationService nlpNormalizationService;
    private final ExtractionLoggingService extractionLoggingService;
    private final EnhancedNumericExtractionService enhancedNumericExtractionService;
    private final ComplexGuaranteeExtractionService complexGuaranteeExtractionService;
    private final FuzzyProductMatcherService fuzzyProductMatcherService;
    private final ActionNormalizationService normalizationService;
    private final BusinessValidationService businessValidationService;
    private final ValidationService validationService;
    private final RecommendationService recommendationService;
    private final ConversationMemoryService conversationMemoryService;
    private final RAGService ragService;

    // Services métier existants (réutilisés)
    private final GarantieService garantieService;
    private final ProduitService produitService;
    private final PackUnifiedService packUnifiedService;

    // Repositories pour la recherche automatique
    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;
    private final PackGarantieRepository packGarantieRepository;

    public ChatbotOrchestratorService(
            PromptAnalyzerService promptAnalyzerService,
            PromptSegmentationService promptSegmentationService,
            AIExtractionService aiExtractionService,
            NLPNormalizationService nlpNormalizationService,
            ExtractionLoggingService extractionLoggingService,
            EnhancedNumericExtractionService enhancedNumericExtractionService,
            ComplexGuaranteeExtractionService complexGuaranteeExtractionService,
            FuzzyProductMatcherService fuzzyProductMatcherService,
            ActionNormalizationService normalizationService,
            BusinessValidationService businessValidationService,
            ValidationService validationService,
            RecommendationService recommendationService,
            ConversationMemoryService conversationMemoryService,
            RAGService ragService,
            GarantieService garantieService,
            ProduitService produitService,
            PackUnifiedService packUnifiedService,
            ProduitRepository produitRepository,PackGarantieRepository packGarantieRepository,
            PackUnifiedRepository packUnifiedRepository,
            GarantieRepository garantieRepository) {
        this.promptAnalyzerService = promptAnalyzerService;
        this.promptSegmentationService = promptSegmentationService;
        this.aiExtractionService = aiExtractionService;
        this.nlpNormalizationService = nlpNormalizationService;
        this.extractionLoggingService = extractionLoggingService;
        this.enhancedNumericExtractionService = enhancedNumericExtractionService;
        this.complexGuaranteeExtractionService = complexGuaranteeExtractionService;
        this.fuzzyProductMatcherService = fuzzyProductMatcherService;
        this.normalizationService = normalizationService;
        this.businessValidationService = businessValidationService;
        this.validationService = validationService;
        this.recommendationService = recommendationService;
        this.conversationMemoryService = conversationMemoryService;
        this.ragService = ragService;
        this.garantieService = garantieService;
        this.produitService = produitService;
        this.packUnifiedService = packUnifiedService;
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
        this.packGarantieRepository = packGarantieRepository;
    }

     //Point d'entrée principal pour le traitement des prompts
    public ChatbotResponseDTO processPrompt(String prompt) {
        return processPrompt(prompt, null);
    }

    //Point d'entrée principal pour le traitement des prompts avec session
    public ChatbotResponseDTO processPrompt(String prompt, String sessionId) {
        logger.info("Début du traitement du prompt: {}", prompt);

        try {
            // Générer ou récupérer l'ID de session
            if (sessionId == null || sessionId.isBlank()) {
                sessionId = conversationMemoryService.generateSessionId();
            }

            // Étape 1: Normalisation NLP avant parsing
            NLPNormalizationService.NormalizationResult normalizationResult = nlpNormalizationService.normalizePrompt(prompt);
            String normalizedPrompt = normalizationResult.getNormalizedText();
            logger.info("Prompt normalisé: {}", normalizedPrompt);

            // Créer le journal d'extraction
            ExtractionLoggingService.ExtractionLog extractionLog = extractionLoggingService.createExtractionLog(
                sessionId, prompt, normalizedPrompt, "INITIAL");

            // Étape 2: Validation du prompt
            ValidationService.ValidationResult promptValidation = validationService.validatePrompt(normalizedPrompt);
            if (!promptValidation.isValid()) {
                extractionLoggingService.logFullReport(extractionLog);
                return createErrorResponse("Prompt invalide", promptValidation.getErrors());
            }

            // Étape 3: Analyse et détection de l'action
            ChatbotAction action = promptAnalyzerService.analyzeAction(normalizedPrompt);
            extractionLog.setAction(action != null ? action.name() : "UNKNOWN");
            
            if (action == null) {
                extractionLoggingService.logFullReport(extractionLog);
                return createErrorResponse("Action non détectée", 
                    List.of("Impossible de déterminer l'action à partir du prompt"));
            }

            // Étape 4: Validation de l'action supportée
            if (!validationService.isSupportedAction(action)) {
                extractionLoggingService.logFullReport(extractionLog);
                return createErrorResponse("Action non supportée", 
                    List.of("L'action '" + action + "' n'est pas encore supportée"));
            }

            // RAG: Rechercher des documents pertinents avant de répondre
            List<RAGService.RAGSearchResult> ragResults = ragService.searchWithFallback(normalizedPrompt);
            String ragContext = ragService.generateContext(ragResults);

            // Enregistrer le message utilisateur dans la mémoire
            conversationMemoryService.addMessageExchange(sessionId, "USER", prompt, action.name(), null);

            // Étape 5: Exécution de l'action avec le prompt normalisé
            Object result = executeAction(action, normalizedPrompt, extractionLog);

            // Calculer le score de confiance global
            double confidenceScore = extractionLoggingService.calculateGlobalConfidence(extractionLog);
            extractionLog.setConfidenceScore(confidenceScore);

            // Étape 6: Création de la réponse
            ChatbotResponseDTO response = createSuccessResponse(action, result, prompt, promptValidation.getWarnings());
            
            // Ajouter sessionId à la réponse
            response.getData().put("sessionId", sessionId);
            
            // Ajouter le contexte RAG si disponible
            if (!ragContext.isBlank()) {
                response.getData().put("ragContext", ragContext);
                response.getData().put("ragResults", ragResults);
            }

            // Ajouter les métadonnées d'extraction
            response.getData().put("extractionConfidence", confidenceScore);
            response.getData().put("normalizedPrompt", normalizedPrompt);
            response.getData().put("recognizedEnums", normalizationResult.getRecognizedEnums());

            // Log le rapport d'extraction complet
            extractionLoggingService.logFullReport(extractionLog);

            // Enregistrer la réponse de l'assistant dans la mémoire
            conversationMemoryService.addMessageExchange(sessionId, "ASSISTANT", response.getMessage(), action.name(), response.getData());

            return response;

        } catch (Exception e) {
            logger.error("Erreur lors du traitement du prompt: {}", e.getMessage(), e);
            return createErrorResponse("Erreur interne", List.of("Une erreur est survenue: " + e.getMessage()));
        }
    }

     // Exécute l'action détectée
    private Object executeAction(ChatbotAction action, String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        logger.info("Exécution de l'action: {}", action);

        return switch (action) {
            case GARANTIE -> executeCreateGarantie(prompt, extractionLog);
            case PRODUIT -> executeCreateProduit(prompt, extractionLog);
            case PACK -> executeCreatePack(prompt, extractionLog);
            case CONFIGURATION_PACK -> executeConfigurePack(prompt, extractionLog);
            case AJOUT_GARANTIE_PACK -> executeAddGarantieToPack(prompt, extractionLog);
            case RECOMMANDATION -> executeRecommendation(prompt, extractionLog);
            default -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Action non implémentée: " + action);
                yield result;
            }
        };
    }

     // Exécute la création d'une garantie
    private Object executeCreateGarantie(String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            extractionLog.setAction("GARANTIE");
            SegmentedPromptDTO segmented = promptSegmentationService.segment(prompt);
            String garantiesText = segmented.getGarantiesSection().isBlank() ? prompt : segmented.getGarantiesSection();

            Map<String, Object> extractedData;
            String extractionMethod;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractGarantieData(prompt);
                extractionMethod = "AI";
            } else {
                extractedData = extractGarantieDataFallback(garantiesText);
                extractionMethod = "REGEX_FALLBACK";
            }

            // Log extraction results
            extractionLoggingService.logFieldExtraction(extractionLog, "nomGarantie", 
                extractedData.get("nom"), extractionMethod, 0.9, extractionMethod, 
                extractedData.get("nom") != null ? extractedData.get("nom").toString() : "");
            extractionLoggingService.logFieldExtraction(extractionLog, "tauxRemboursement", 
                extractedData.get("tauxRemboursement"), extractionMethod, 0.85, extractionMethod, "");
            
            // Use enhanced numeric extraction for better accuracy
            EnhancedNumericExtractionService.NumericExtractionResult tauxResult = 
                enhancedNumericExtractionService.extractTauxRemboursement(garantiesText);
            if (tauxResult.isValid() && tauxResult.getConfidence() > 0.8) {
                extractedData.put("tauxRemboursement", tauxResult.getValue());
                extractionLoggingService.logFieldExtraction(extractionLog, "tauxRemboursement_enhanced", 
                    tauxResult.getValue(), "ENHANCED_NUMERIC", tauxResult.getConfidence(), "ENHANCED", 
                    tauxResult.getRawMatch());
            }

            EnhancedNumericExtractionService.NumericExtractionResult franchiseResult = 
                enhancedNumericExtractionService.extractFranchise(garantiesText);
            if (franchiseResult.isValid()) {
                extractedData.put("franchise", franchiseResult.getValue());
                extractionLoggingService.logFieldExtraction(extractionLog, "franchise_enhanced", 
                    franchiseResult.getValue(), "ENHANCED_NUMERIC", franchiseResult.getConfidence(), "ENHANCED", 
                    franchiseResult.getRawMatch());
            }

            ChatbotGarantieRequestDTO dto = createGarantieDTO(extractedData, prompt, garantiesText);

            // Normalisation
            Garantie garantie = normalizeAndConvertToGarantie(dto);

            // Validation
            ValidationService.ValidationResult validation = validationService.validateGarantie(garantie);
            if (!validation.isValid()) {
                extractionLog.getErrors().addAll(validation.getErrors());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Validation échouée");
                result.put("details", validation.getErrors());
                result.put("warnings", validation.getWarnings());
                return result;
            }

            // Business validation before score calculation
            BusinessValidationService.BusinessValidationResult businessValidation = 
                businessValidationService.validateGarantieForScoring(garantie, 1.0);
            
            if (!businessValidation.isValid()) {
                extractionLog.getWarnings().addAll(businessValidation.getValidationWarnings());
                extractionLog.getWarnings().addAll(businessValidation.getSuggestions());
            }

            // Application des valeurs par défaut (seulement si non détecté)
            normalizationService.applyGarantieDefaults(garantie);

            // Création via le service métier existant
            Garantie created = garantieService.createGarantie(garantie);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", "CREATE_GARANTIE");
            result.put("entity", created);
            result.put("message", "Garantie créée avec succès");
            result.put("id", created.getIdGarantie());
            result.put("warnings", validation.getWarnings());
            result.put("businessValidation", businessValidationService.generateValidationMessage(businessValidation));
            return result;

        } catch (Exception e) {
            logger.error("Erreur création garantie: {}", e.getMessage(), e);
            extractionLog.addError("Erreur création garantie: " + e.getMessage());
            return Map.of("success", false, "error", "Erreur création garantie: " + e.getMessage());
        }
    }
     // Exécute la création d'un produit
    private Object executeCreateProduit(String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            extractionLog.setAction("PRODUIT");
            // Extraction des données
            Map<String, Object> extractedData;
            String extractionMethod;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractProduitData(prompt);
                extractionMethod = "AI";
            } else {
                extractedData = extractProduitDataFallback(prompt);
                extractionMethod = "REGEX_FALLBACK";
            }

            // Log extraction results
            extractionLoggingService.logFieldExtraction(extractionLog, "nomProduit", 
                extractedData.get("nom"), extractionMethod, 0.9, extractionMethod, 
                extractedData.get("nom") != null ? extractedData.get("nom").toString() : "");
            extractionLoggingService.logFieldExtraction(extractionLog, "typeProduit", 
                extractedData.get("typeProduit"), extractionMethod, 0.85, extractionMethod, "");

            // Création du dto
            ChatbotProduitRequestDTO dto = createProduitDTO(extractedData, prompt);

            // Normalisation
            Produit produit = normalizeAndConvertToProduit(dto);

            // Validation
            ValidationService.ValidationResult validation = validationService.validateProduit(produit);
            if (!validation.isValid()) {
                extractionLog.getErrors().addAll(validation.getErrors());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Validation échouée");
                result.put("details", validation.getErrors());
                result.put("warnings", validation.getWarnings());
                return result;
            }

            // Business validation before score calculation
            BusinessValidationService.BusinessValidationResult businessValidation = 
                businessValidationService.validateProduitForScoring(produit, 1.0);
            
            if (!businessValidation.isValid()) {
                extractionLog.getWarnings().addAll(businessValidation.getValidationWarnings());
                extractionLog.getWarnings().addAll(businessValidation.getSuggestions());
            }

            // Application des valeurs par défaut (seulement si non détecté)
            normalizationService.applyProduitDefaults(produit);

            // Création via le service métier existant
            Produit created = produitService.createProduit(produit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", "CREATE_PRODUIT");
            result.put("entity", created);
            result.put("message", "Produit créé avec succès");
            result.put("id", created.getIdProduit());
            result.put("warnings", validation.getWarnings());
            result.put("businessValidation", businessValidationService.generateValidationMessage(businessValidation));
            return result;

        } catch (Exception e) {
            logger.error("Erreur création produit: {}", e.getMessage(), e);
            extractionLog.addError("Erreur création produit: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Erreur création produit: " + e.getMessage());
            return result;
        }
    }

     //Exécute la création d'un pack
    private Object executeCreatePack(String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            extractionLog.setAction("PACK");
            SegmentedPromptDTO segmented = promptSegmentationService.segment(prompt);
            String packText = segmented.getPackSection().isBlank() ? prompt : segmented.getPackSection();

            Map<String, Object> extractedData;
            String extractionMethod;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractPackData(prompt);
                mergePackRegexFallback(extractedData, packText);
                extractionMethod = "AI";
            } else {
                extractedData = extractPackDataFallback(packText);
                extractionMethod = "REGEX_FALLBACK";
            }

            // Log extraction results
            extractionLoggingService.logFieldExtraction(extractionLog, "nomPack", 
                extractedData.get("nom"), extractionMethod, 0.9, extractionMethod, 
                extractedData.get("nom") != null ? extractedData.get("nom").toString() : "");
            
            // Use enhanced numeric extraction for better accuracy
            EnhancedNumericExtractionService.NumericExtractionResult prixResult = 
                enhancedNumericExtractionService.extractPrixMensuel(packText);
            if (prixResult.isValid() && prixResult.getConfidence() > 0.8) {
                extractedData.put("prixMensuel", prixResult.getValue());
                extractionLoggingService.logFieldExtraction(extractionLog, "prixMensuel_enhanced", 
                    prixResult.getValue(), "ENHANCED_NUMERIC", prixResult.getConfidence(), "ENHANCED", 
                    prixResult.getRawMatch());
            }

            EnhancedNumericExtractionService.NumericExtractionResult ageMinResult = 
                enhancedNumericExtractionService.extractAgeMinimum(packText);
            if (ageMinResult.isValid()) {
                extractedData.put("ageMinimum", ageMinResult.getValue());
                extractionLoggingService.logFieldExtraction(extractionLog, "ageMinimum_enhanced", 
                    ageMinResult.getValue(), "ENHANCED_NUMERIC", ageMinResult.getConfidence(), "ENHANCED", 
                    ageMinResult.getRawMatch());
            }

            EnhancedNumericExtractionService.NumericExtractionResult ageMaxResult = 
                enhancedNumericExtractionService.extractAgeMaximum(packText);
            if (ageMaxResult.isValid()) {
                extractedData.put("ageMaximum", ageMaxResult.getValue());
                extractionLoggingService.logFieldExtraction(extractionLog, "ageMaximum_enhanced", 
                    ageMaxResult.getValue(), "ENHANCED_NUMERIC", ageMaxResult.getConfidence(), "ENHANCED", 
                    ageMaxResult.getRawMatch());
            }

            // Extract duration phrases using NLP normalization for natural language expressions
            Map<String, Object> durations = nlpNormalizationService.extractDurationPhrases(packText);
            if (durations.containsKey("dureeMinContrat")) {
                extractedData.put("dureeMinContrat", durations.get("dureeMinContrat"));
                extractionLoggingService.logFieldExtraction(extractionLog, "dureeMinContrat_nlp", 
                    durations.get("dureeMinContrat"), "NLP_DURATION", 0.85, "NLP", "Natural language duration extraction");
            }
            if (durations.containsKey("dureeMaxContrat")) {
                extractedData.put("dureeMaxContrat", durations.get("dureeMaxContrat"));
                extractionLoggingService.logFieldExtraction(extractionLog, "dureeMaxContrat_nlp", 
                    durations.get("dureeMaxContrat"), "NLP_DURATION", 0.85, "NLP", "Natural language duration extraction");
            }
            if (durations.containsKey("ageMinimum")) {
                extractedData.put("ageMinimum", durations.get("ageMinimum"));
                extractionLoggingService.logFieldExtraction(extractionLog, "ageMinimum_nlp", 
                    durations.get("ageMinimum"), "NLP_DURATION", 0.85, "NLP", "Natural language age extraction");
            }

            // Extract complex guarantee list with detailed attributes
            List<ComplexGuaranteeExtractionService.ExtractedGuarantee> complexGuarantees = 
                complexGuaranteeExtractionService.extractComplexGuarantees(packText);
            
            if (!complexGuarantees.isEmpty()) {
                extractedData.put("complexGaranties", complexGuarantees);
                extractionLog.addMetadata("complexGarantiesCount", complexGuarantees.size());
                extractionLog.addMetadata("complexGuantiesData", complexGuarantees);
            }

            ChatbotPackRequestDTO dto = createPackDTO(extractedData, prompt, packText);

            // Use fuzzy product matching for better product association
            linkPackToExistingProduitWithFuzzy(dto, packText, extractionLog);

            Pack pack = normalizeAndConvertToPack(dto);

            // Validation
            ValidationService.ValidationResult validation = validationService.validatePack(pack);
            if (!validation.isValid()) {
                extractionLog.getErrors().addAll(validation.getErrors());
                PackCreationResponseDTO errorResponse = new PackCreationResponseDTO(false, "Validation échouée");
                errorResponse.setErrors(validation.getErrors());
                errorResponse.setWarnings(validation.getWarnings());
                errorResponse.setPackDetails(PackCreationResponseDTO.PackDetails.fromEntity(pack));
                return errorResponse;
            }

            // Business validation before score calculation
            BusinessValidationService.BusinessValidationResult businessValidation =
                businessValidationService.validatePackForScoring(pack, 1.0);

            if (!businessValidation.isValid()) {
                extractionLog.getWarnings().addAll(businessValidation.getValidationWarnings());
                extractionLog.getWarnings().addAll(businessValidation.getSuggestions());
            }

            // Vérifier si des champs critiques manquent avant création
            List<String> missingFields = businessValidation.getMissingCriticalFields().keySet().stream().toList();
            if (!missingFields.isEmpty()) {
                PackCreationResponseDTO confirmationResponse = new PackCreationResponseDTO(
                    false,
                    "Veuillez confirmer les informations suivantes pour créer le pack '" + pack.getNomPack() + "':"
                );
                confirmationResponse.setErrors(missingFields);
                confirmationResponse.setWarnings(validation.getWarnings());
                businessValidation.getValidationWarnings().forEach(confirmationResponse::addWarning);
                businessValidation.getSuggestions().forEach(s -> confirmationResponse.addWarning("Suggestion: " + s));
                confirmationResponse.setPackDetails(PackCreationResponseDTO.PackDetails.fromEntity(pack));
                confirmationResponse.setMessage("Champs manquants: " + String.join(", ", missingFields));
                return confirmationResponse;
            }

            // Application des valeurs par défaut (seulement si non détecté)
            normalizationService.applyPackDefaults(pack);

            // Résolution du nom de produit en ID si nécessaire
            if ((pack.getProduitId() == null || pack.getProduitId().isBlank()) && dto.getNomProduit() != null && !dto.getNomProduit().isBlank()) {
                try {
                    List<tn.vermeg.gestionproduit.entities.Produit> matchingProduits = produitRepository.findByNomProduitContainingIgnoreCase(dto.getNomProduit());
                    if (!matchingProduits.isEmpty()) {
                        pack.setProduitId(matchingProduits.get(0).getIdProduit());
                        logger.info("Produit associé par nom: {} → ID: {}", dto.getNomProduit(), pack.getProduitId());
                    } else {
                        extractionLog.addWarning("Produit non trouvé avec le nom: " + dto.getNomProduit());
                    }
                } catch (Exception e) {
                    logger.warn("Erreur lors de la résolution du produit par nom: {}", e.getMessage());
                    extractionLog.addWarning("Impossible de résoudre le produit par nom: " + dto.getNomProduit());
                }
            }

            // Création via le service métier existant
            Pack created = packUnifiedService.createPack(pack);

            // Validation blocante: si aucune garantie n'est spécifiée, refuser la création
            if ((dto.getComplexGaranties() == null || dto.getComplexGaranties().isEmpty()) && 
                (dto.getGaranties() == null || dto.getGaranties().isEmpty())) {
                PackCreationResponseDTO errorResponse = new PackCreationResponseDTO(false, "Erreur: Aucune garantie spécifiée");
                errorResponse.setErrors(List.of("Le pack doit contenir au moins une garantie. Veuillez spécifier les garanties dans votre demande."));
                errorResponse.setPackDetails(PackCreationResponseDTO.PackDetails.fromEntity(created));
                logger.warn("Création de pack refusée: aucune garantie spécifiée pour le pack '{}'", created.getNomPack());
                return errorResponse;
            }

            // Associer les garanties extraites au pack
            if (dto.getComplexGaranties() != null && !dto.getComplexGaranties().isEmpty()) {
                associateComplexGarantiesToPack(created.getIdPack(), dto.getComplexGaranties(), extractionLog);
            } else if (dto.getGaranties() != null && !dto.getGaranties().isEmpty()) {
                associateGarantiesToPack(created.getIdPack(), dto.getGaranties());
            }

            // Construire la réponse structurée
            PackCreationResponseDTO response = buildStructuredPackResponse(created, dto, validation, businessValidation, extractionLog);
            return response;

        } catch (Exception e) {
            logger.error("Erreur création pack: {}", e.getMessage(), e);
            extractionLog.addError("Erreur création pack: " + e.getMessage());
            PackCreationResponseDTO errorResponse = new PackCreationResponseDTO(false, "Erreur création pack: " + e.getMessage());
            errorResponse.addError("Erreur création pack: " + e.getMessage());
            return errorResponse;
        }
    }
     //Exécute la configuration d'un pack
    private Object executeConfigurePack(String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            extractionLog.setAction("CONFIGURATION_PACK");
            // Extraction des données
            Map<String, Object> extractedData;
            String extractionMethod;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractPackConfigurationData(prompt);
                extractionMethod = "AI";
            } else {
                extractedData = extractPackConfigurationDataFallback(prompt);
                extractionMethod = "REGEX_FALLBACK";
            }

            // Création de l'entité PackGarantie
            PackGarantie packGarantie = createPackGarantieFromData(extractedData);

            // Validation
            ValidationService.ValidationResult validation = validationService.validatePackConfiguration(packGarantie);
            if (!validation.isValid()) {
                extractionLog.getErrors().addAll(validation.getErrors());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Validation échouée");
                result.put("details", validation.getErrors());
                result.put("warnings", validation.getWarnings());
                return result;
            }

            // Recherche des IDs
            String packId = findPackId(extractedData);
            String garantieId = findGarantieId(extractedData);

            if (packId == null || garantieId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Entités non trouvées");
                result.put("details", List.of("Pack ou garantie non trouvé(e) avec les informations fournies"));
                return result;
            }

            // Configuration via le service métier existant
            PackGarantie created = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", "CONFIGURE_PACK");
            result.put("entity", created);
            result.put("message", "Pack configuré avec succès");
            result.put("id", created.getIdPackGarantie());
            return result;

        } catch (Exception e) {
            logger.error("Erreur configuration pack: {}", e.getMessage(), e);
            extractionLog.addError("Erreur configuration pack: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Erreur configuration pack: " + e.getMessage());
            return result;
        }
    }

     // Exécute l'ajout de garantie à un pack
    private Object executeAddGarantieToPack(String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            extractionLog.setAction("AJOUT_GARANTIE_PACK");
            // Extraction des données
            Map<String, Object> extractedData;
            String extractionMethod;
            if (aiExtractionService.isAIAvailable()) {
                extractedData = aiExtractionService.extractAddGarantieToPackData(prompt);
                extractionMethod = "AI";
            } else {
                extractedData = extractAddGarantieToPackDataFallback(prompt);
                extractionMethod = "REGEX_FALLBACK";
            }

            // Création de l'entité PackGarantie
            PackGarantie packGarantie = createPackGarantieFromData(extractedData);

            // Validation
            ValidationService.ValidationResult validation = validationService.validatePackConfiguration(packGarantie);
            if (!validation.isValid()) {
                extractionLog.getErrors().addAll(validation.getErrors());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Validation échouée");
                result.put("details", validation.getErrors());
                result.put("warnings", validation.getWarnings());
                return result;
            }

            // Recherche des IDs
            String packId = findPackId(extractedData);
            String garantieId = findGarantieId(extractedData);

            if (packId == null || garantieId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Entités non trouvées");
                result.put("details", List.of("Pack ou garantie non trouvé(e) avec les informations fournies"));
                return result;
            }

            // Ajout via le service métier existant
            PackGarantie created = packUnifiedService.ajouterGarantieAuPack(packId, garantieId, packGarantie);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", "ADD_GARANTIE_TO_PACK");
            result.put("entity", created);
            result.put("message", "Garantie ajoutée au pack avec succès");
            result.put("id", created.getIdPackGarantie());
            return result;

        } catch (Exception e) {
            logger.error("Erreur ajout garantie au pack: {}", e.getMessage(), e);
            extractionLog.addError("Erreur ajout garantie au pack: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Erreur ajout garantie au pack: " + e.getMessage());
            return result;
        }
    }

    // Exécute la recommandation intelligente
    private Object executeRecommendation(String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            extractionLog.setAction("RECOMMANDATION");
            logger.info("Exécution de la recommandation pour le prompt: {}", prompt);
            
            // Détecter et gérer les profils multiples dans le prompt
            String processedPrompt = handleMultipleProfiles(prompt);
            
            // Extraire les informations de profil du prompt
            Map<String, Object> profileInfo = extractProfileFromPrompt(processedPrompt);
            
            // Log profile extraction
            extractionLoggingService.logFieldExtraction(extractionLog, "profile_age", 
                profileInfo.get("age"), "PROFILE_EXTRACTION", 0.85, "PROFILE", "");
            extractionLoggingService.logFieldExtraction(extractionLog, "profile_budget", 
                profileInfo.get("monthlyBudget"), "PROFILE_EXTRACTION", 0.85, "PROFILE", "");
            
            // Créer un RecommendationRequestDTO à partir du prompt
            tn.vermeg.gestionproduit.dto.RecommendationRequestDTO request = 
                new tn.vermeg.gestionproduit.dto.RecommendationRequestDTO();
            request.setSessionId(UUID.randomUUID().toString());
            
            // Remplir le request avec les informations extraites du profil
            if (profileInfo.containsKey("age")) {
                request.setAge((Integer) profileInfo.get("age"));
            }
            if (profileInfo.containsKey("gender")) {
                request.setGender((String) profileInfo.get("gender"));
            }
            if (profileInfo.containsKey("maritalStatus")) {
                request.setMaritalStatus((String) profileInfo.get("maritalStatus"));
            }
            if (profileInfo.containsKey("numberOfChildren")) {
                request.setNumberOfChildren((Integer) profileInfo.get("numberOfChildren"));
            }
            if (profileInfo.containsKey("monthlyBudget")) {
                request.setMonthlyBudget((Double) profileInfo.get("monthlyBudget"));
            }
            if (profileInfo.containsKey("chronicDiseases")) {
                request.setChronicDiseases((List<String>) profileInfo.get("chronicDiseases"));
            }
            
            tn.vermeg.gestionproduit.dto.RecommendationResponseDTO recommendationResponse = 
                recommendationService.generateRecommendations(request);
            
            logger.info("RecommendationResponse - Success: {}, RecommendedPacks count: {}, Has explanation: {}", 
                recommendationResponse.isSuccess(),
                recommendationResponse.getRecommendedPacks() != null ? recommendationResponse.getRecommendedPacks().size() : 0,
                recommendationResponse.getExplanation() != null && !recommendationResponse.getExplanation().isEmpty());
            
            if (recommendationResponse.isSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("action", "RECOMMANDATION");
                result.put("result", recommendationResponse);
                result.put("message", "Recommandations générées avec succès");
                result.put("profileProcessed", profileInfo);
                return result;
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Erreur lors de la génération des recommandations");
                result.put("message", recommendationResponse.getMessage());
                return result;
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de la recommandation: {}", e.getMessage(), e);
            extractionLog.addError("Erreur recommandation: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Erreur recommandation: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Détecte et gère les profils multiples dans le prompt.
     * Si plusieurs profils sont détectés, garde uniquement le premier.
     */
    private String handleMultipleProfiles(String prompt) {
        // Détecter les occurrences de "Je suis" qui indiquent des profils multiples
        String[] profileMarkers = {"Je suis", "je suis", "Je suis un", "je suis un", "Je suis une", "je suis une"};
        int profileCount = 0;
        
        for (String marker : profileMarkers) {
            int index = prompt.indexOf(marker);
            while (index != -1) {
                profileCount++;
                index = prompt.indexOf(marker, index + 1);
            }
        }
        
        if (profileCount > 1) {
            logger.warn("Profils multiples détectés dans le prompt ({} profils). Conservation du premier profil uniquement.", profileCount);
            
            // Extraire le premier profil (jusqu'au deuxième "Je suis" ou fin du prompt)
            for (String marker : profileMarkers) {
                int firstIndex = prompt.indexOf(marker);
                if (firstIndex != -1) {
                    int secondIndex = prompt.indexOf(marker, firstIndex + 1);
                    if (secondIndex != -1) {
                        return prompt.substring(0, secondIndex).trim();
                    }
                }
            }
        }
        
        return prompt;
    }
    
    /**
     * Extrait les informations de profil du prompt pour la recommandation.
     */
    private Map<String, Object> extractProfileFromPrompt(String prompt) {
        Map<String, Object> profile = new HashMap<>();
        
        // Extraire l'âge
        Integer age = promptAnalyzerService.extractAge(prompt);
        if (age != null) {
            profile.put("age", age);
        }
        
        // Extraire le genre
        if (prompt.toLowerCase().contains("homme") || prompt.toLowerCase().contains("marié")) {
            profile.put("gender", "M");
        } else if (prompt.toLowerCase().contains("femme") || prompt.toLowerCase().contains("mariée")) {
            profile.put("gender", "F");
        }
        
        // Extraire le statut marital
        if (prompt.toLowerCase().contains("marié") || prompt.toLowerCase().contains("mariée")) {
            profile.put("maritalStatus", "MARRIED");
        } else if (prompt.toLowerCase().contains("célibataire")) {
            profile.put("maritalStatus", "SINGLE");
        }
        
        // Extraire le nombre d'enfants
        Integer children = promptAnalyzerService.extractNumberOfChildren(prompt);
        if (children != null) {
            profile.put("numberOfChildren", children);
        }
        
        // Extraire le budget mensuel
        Double budget = promptAnalyzerService.extractBudget(prompt);
        if (budget != null) {
            profile.put("monthlyBudget", budget);
        }
        
        // Extraire les maladies chroniques
        List<String> chronicDiseases = promptAnalyzerService.extractChronicDiseases(prompt);
        if (chronicDiseases != null && !chronicDiseases.isEmpty()) {
            profile.put("chronicDiseases", chronicDiseases);
        }
        
        logger.info("Profil extrait du prompt: {}", profile);
        return profile;
    }

    // Méthodes utilitaires de conversion et normalisation
    private ChatbotGarantieRequestDTO createGarantieDTO(Map<String, Object> extractedData, String prompt, String analysisText) {
        ChatbotGarantieRequestDTO dto = new ChatbotGarantieRequestDTO();
        dto.setOriginalPrompt(prompt);
        
        // Nettoyer le nom pour supprimer "nommee" si présent
        String nomFromAI = aiExtractionService.getStringValue(extractedData, "nom", promptAnalyzerService.extractNomGarantie(prompt));
        if (nomFromAI != null && nomFromAI.toLowerCase().startsWith("nommee ")) {
            nomFromAI = nomFromAI.substring(7).trim();
        }
        dto.setNomGarantie(nomFromAI);
        dto.setDescription(aiExtractionService.getStringValue(extractedData, "description", promptAnalyzerService.extractDescription(prompt)));

        // Utiliser le prompt complet pour l'extraction du domaine
        String domaineStr = aiExtractionService.getStringValue(extractedData, "domaine", null);
        if (domaineStr == null || domaineStr.isBlank()) {
            domaineStr = prompt;
        }
        dto.setDomaine(normalizationService.normalizeDomaineMedical(domaineStr));

        dto.setTauxRemboursement(aiExtractionService.getDoubleValue(extractedData, "tauxRemboursement", promptAnalyzerService.extractTauxRemboursement(prompt)));
        // Utiliser le prompt complet pour le type montant (IA non fiable)
        dto.setTypeMontant(promptAnalyzerService.extractTypeMontant(prompt));
        
        PromptAnalyzerService.PlafondData plafonds = promptAnalyzerService.extractPlafonds(prompt);
        dto.setPlafondAnnuel(aiExtractionService.getDoubleValue(extractedData, "plafondAnnuel", plafonds.annuel));
        dto.setPlafondMensuel(aiExtractionService.getDoubleValue(extractedData, "plafondMensuel", plafonds.mensuel));
        dto.setPlafondParActe(aiExtractionService.getDoubleValue(extractedData, "plafondParActe", plafonds.parActe));
        
        dto.setFranchise(
            Optional.ofNullable(promptAnalyzerService.extractFranchise(prompt)).orElse(0.0)
        );
        dto.setCoutMoyenParSinistre(aiExtractionService.getDoubleValue(extractedData, "coutMoyenParSinistre", promptAnalyzerService.extractCoutMoyenParSinistre(prompt)));
        // Utiliser le prompt complet pour les durées (IA non fiable)
        dto.setDureeMinContrat(promptAnalyzerService.extractDureeMinContrat(prompt));
        dto.setDureeMaxContrat(promptAnalyzerService.extractDureeMaxContrat(prompt));
        
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
        garantie.setDomaine(dto.getDomaine());
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
        
        // Utiliser uniquement le regex pour la description (IA non fiable)
        dto.setDescription(promptAnalyzerService.extractDescription(packText));
        
        dto.setAgeMinimum(aiExtractionService.getIntegerValue(extractedData, "ageMinimum", promptAnalyzerService.extractAgeMinimum(packText)));
        dto.setAgeMaximum(aiExtractionService.getIntegerValue(extractedData, "ageMaximum", promptAnalyzerService.extractAgeMaximum(packText)));
        dto.setAncienneteContratMois(aiExtractionService.getIntegerValue(extractedData, "ancienneteContratMois", promptAnalyzerService.extractAncienneteContrat(packText)));
        CouvertureGeographique couvertureValue = normalizationService.normalizeCouvertureGeographique(
            aiExtractionService.getStringValue(extractedData, "couvertureGeographique", promptAnalyzerService.extractCouvertureGeographique(packText)));
        dto.setCouvertureGeographique(couvertureValue != null ? couvertureValue.toString() : null);
        dto.setPrixMensuel(aiExtractionService.getDoubleValue(extractedData, "prixMensuel", promptAnalyzerService.extractPrixMensuel(packText)));
        
        // Utiliser les durées extraites via NLP avec fallback sur regex
        Integer dureeMinNlp = null;
        Integer dureeMaxNlp = null;
        
        if (extractedData.containsKey("dureeMinContrat") && extractedData.get("dureeMinContrat") != null) {
            dureeMinNlp = aiExtractionService.getIntegerValue(extractedData, "dureeMinContrat", 0);
            if (dureeMinNlp == 0) dureeMinNlp = null;
        }
        if (extractedData.containsKey("dureeMaxContrat") && extractedData.get("dureeMaxContrat") != null) {
            dureeMaxNlp = aiExtractionService.getIntegerValue(extractedData, "dureeMaxContrat", 0);
            if (dureeMaxNlp == 0) dureeMaxNlp = null;
        }
        
        dto.setDureeMinContrat(dureeMinNlp != null ? dureeMinNlp : promptAnalyzerService.extractDureeMinContrat(packText));
        dto.setDureeMaxContrat(dureeMaxNlp != null ? dureeMaxNlp : promptAnalyzerService.extractDureeMaxContrat(packText));
        
        NiveauCouverture niveauValue = normalizationService.normalizeNiveauCouverture(
            aiExtractionService.getStringValue(extractedData, "niveauCouverture", promptAnalyzerService.extractNiveauCouverture(packText)));
        dto.setNiveauCouverture(niveauValue != null ? niveauValue.toString() : null);
        Statut statutValue3 = normalizationService.normalizeStatut(aiExtractionService.getStringValue(extractedData, "statut", null));
        dto.setStatut(statutValue3 != null ? statutValue3.toString() : null);
        dto.setProduitId(aiExtractionService.getStringValue(extractedData, "produitId", null));
        
        // Forcer l'utilisation du regex pour le nom du produit si l'IA ne retourne rien
        String nomProduitFromAI = aiExtractionService.getStringValue(extractedData, "nomProduit", null);
        String nomProduitFromRegex = extractProduitNameFromPrompt(packText);
        dto.setNomProduit((nomProduitFromAI != null && !nomProduitFromAI.isBlank()) ? nomProduitFromAI : nomProduitFromRegex);

        List<String> fromAi = extractTypeClientStringsFromMap(extractedData);
        List<String> fromRegex = promptAnalyzerService.extractTypeClientLabels(packText);
        if (!fromAi.isEmpty()) {
            dto.setTypeClients(normalizationService.normalizeTypeClients(fromAi));
        } else if (!fromRegex.isEmpty()) {
            dto.setTypeClients(normalizationService.normalizeTypeClients(fromRegex));
        }

        // Extraire les garanties du prompt
        List<String> garanties = promptAnalyzerService.extractGaranties(packText);
        dto.setGaranties(garanties != null ? new ArrayList<>(garanties) : new ArrayList<>());

        // Preserve complex guarantee data if available
        if (extractedData.containsKey("complexGaranties")) {
            dto.setComplexGaranties((List<ComplexGuaranteeExtractionService.ExtractedGuarantee>) extractedData.get("complexGaranties"));
        }

        // Extract domaines medicaux if available
        if (extractedData.containsKey("domainesMedicaux")) {
            Object domainesObj = extractedData.get("domainesMedicaux");
            if (domainesObj instanceof List) {
                dto.setDomainesMedicaux((List<String>) domainesObj);
            }
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
        pack.setDomainesMedicaux(dto.getDomainesMedicaux() != null ? dto.getDomainesMedicaux() : new java.util.ArrayList<>());
        return pack;
    }

    // Associe les garanties extraites du prompt au pack
    private void associateGarantiesToPack(String packId, List<String> garantieNames) {
        if (packId == null || garantieNames == null || garantieNames.isEmpty()) {
            return;
        }

        try {
            for (String garantieName : garantieNames) {
                // Rechercher la garantie par nom
                Garantie garantie = garantieRepository.findByNomGarantieContainingIgnoreCase(garantieName)
                    .stream()
                    .findFirst()
                    .orElse(null);

                if (garantie != null) {
                    // Créer l'association PackGarantie avec des valeurs par défaut
                    PackGarantie packGarantie = new PackGarantie();
                    packGarantie.setPackId(packId);
                    packGarantie.setGarantieId(garantie.getIdGarantie());
                    packGarantie.setNomGarantie(garantie.getNomGarantie());
                    
                    // Utiliser les valeurs de la garantie ou des valeurs par défaut
                    Double tauxRemboursement = garantie.getTauxRemboursement();
                    packGarantie.setTauxRemboursement(tauxRemboursement != null && tauxRemboursement > 0 ? tauxRemboursement : 0.8);
                    
                    Double plafondAnnuel = garantie.getPlafondAnnuel();
                    packGarantie.setPlafond(plafondAnnuel != null && plafondAnnuel > 0 ? plafondAnnuel : 10000);
                    
                    Double franchise = garantie.getFranchise();
                    packGarantie.setFranchise(franchise != null ? franchise : 0);
                    
                    packGarantie.setTypeMontant(garantie.getTypeMontant() != null ? garantie.getTypeMontant() : TypeMontant.TARIF_CONVENTIONNE);
                    packGarantie.setDelaiCarence(0);
                    packGarantie.setPriorite(1);
                    packGarantie.setActif(true);
                    packGarantie.setDateActivation(java.time.Instant.now());
                    packGarantie.setOptionnelle(false);
                    packGarantie.setSupplementPrix(0);

                    // Ajouter l'association
                    packUnifiedService.ajouterGarantieAuPack(packId, garantie.getIdGarantie(), packGarantie);
                    logger.info("Garantie '{}' associée au pack '{}'", garantieName, packId);
                } else {
                    logger.warn("Garantie '{}' non trouvée, impossible de l'associer au pack '{}'", garantieName, packId);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'association des garanties au pack {}: {}", packId, e.getMessage(), e);
        }
    }

     // Associe les garanties complexes extraites avec leurs attributs détaillés
     private void associateComplexGarantiesToPack(String packId, List<ComplexGuaranteeExtractionService.ExtractedGuarantee> complexGuarantees, ExtractionLoggingService.ExtractionLog extractionLog) {
         if (packId == null || complexGuarantees == null || complexGuarantees.isEmpty()) {
             return;
         }

         try {
             for (ComplexGuaranteeExtractionService.ExtractedGuarantee extractedGuarantee : complexGuarantees) {
                 Garantie garantie = garantieRepository.findByNomGarantieContainingIgnoreCase(extractedGuarantee.getName())
                         .stream()
                         .findFirst()
                         .orElse(null);

                 // Si la garantie n'existe pas, la créer automatiquement
                 if (garantie == null) {
                     garantie = createGaranteeFromExtractedData(extractedGuarantee, extractionLog);
                     if (garantie != null) {
                         logger.info("Garantie '{}' créée automatiquement", extractedGuarantee.getName());
                         extractionLog.addMetadata("created_guarantee_" + extractedGuarantee.getName(), extractedGuarantee);
                     } else {
                         logger.error("Échec de création de la garantie '{}'", extractedGuarantee.getName());
                         extractionLog.addError("Échec de création de la garantie: " + extractedGuarantee.getName());
                         continue; // Passer à la garantie suivante
                     }
                 }

                 if (garantie != null) {
                     PackGarantie packGarantie = new PackGarantie();
                     packGarantie.setPackId(packId);
                     packGarantie.setGarantieId(garantie.getIdGarantie());
                     packGarantie.setNomGarantie(garantie.getNomGarantie());
// Utiliser les valeurs extraites avec fallback sur les valeurs de la garantie
                     Double extractedTaux = extractedGuarantee.getTauxRemboursement();
                     packGarantie.setTauxRemboursement(extractedTaux != null && extractedTaux > 0 ? extractedTaux :
                             ( garantie.getTauxRemboursement() > 0 ? garantie.getTauxRemboursement() : 0.8));

                     Double extractedPlafond = extractedGuarantee.getPlafond();
                     packGarantie.setPlafond(extractedPlafond != null && extractedPlafond > 0 ? extractedPlafond :
                             ( garantie.getPlafondAnnuel() > 0 ? garantie.getPlafondAnnuel() : 10000));

                     Double extractedFranchise = extractedGuarantee.getFranchise();
                     packGarantie.setFranchise(extractedFranchise != null && extractedFranchise > 0 ? extractedFranchise :
                             (garantie.getFranchise() != 0 ? garantie.getFranchise() : 0));

                     packGarantie.setTypeMontant(garantie.getTypeMontant() != null ? garantie.getTypeMontant() : TypeMontant.TARIF_CONVENTIONNE);
                     packGarantie.setDelaiCarence(0);
                     packGarantie.setPriorite(extractedGuarantee.getPriority() > 0 ? extractedGuarantee.getPriority() : 1);
                     packGarantie.setActif(true);
                     packGarantie.setDateActivation(java.time.Instant.now());
                     packGarantie.setOptionnelle(extractedGuarantee.isOptionnelle());
                     Double extractedSupplement = extractedGuarantee.getSupplementPrix();
                     packGarantie.setSupplementPrix(extractedSupplement != null && extractedSupplement > 0 ? extractedSupplement : 0);
                     packUnifiedService.ajouterGarantieAuPack(packId, garantie.getIdGarantie(), packGarantie);
                     logger.info("Garantie complexe '{}' associée au pack '{}' avec taux: {}, plafond: {}",
                             extractedGuarantee.getName(), packId, packGarantie.getTauxRemboursement(), packGarantie.getPlafond());
                     extractionLog.addMetadata("associated_guarantee_" + extractedGuarantee.getName(), extractedGuarantee);
                 } else {
                     logger.warn("Garantie '{}' non trouvée, impossible de l'associer au pack '{}'", extractedGuarantee.getName(), packId);
                     extractionLog.addWarning("Garantie '" + extractedGuarantee.getName() + "' non trouvée dans la base");
                 }
             }
         } catch (Exception e) {
             logger.error("Erreur lors de l'association des garanties complexes au pack {}: {}", packId, e.getMessage(), e);
             extractionLog.addError("Erreur association garanties complexes: " + e.getMessage());
         }
     }
    private void linkPackToExistingProduit(ChatbotPackRequestDTO dto, String prompt) {
        // Extraire le NOM du produit spécifiquement, pas le TYPE
        String nomProduit = extractProduitNameFromPrompt(prompt);

        // Si aucun nom de produit n'est spécifié, ne pas lier automatiquement
        if (nomProduit == null || nomProduit.isBlank()) {
            // Ne pas injecter de produit générique - laisser le pack sans produit lié
            dto.addWarning("Aucun produit spécifié pour la liaison. Le pack sera créé sans produit lié.");
            return;
        }

        // Rechercher le produit par NOM exact ou partiel
        Optional<Produit> existingProduit = produitRepository.findAll().stream()
            .filter(p -> p.getNomProduit().toLowerCase().contains(nomProduit.toLowerCase()) ||
                       nomProduit.toLowerCase().contains(p.getNomProduit().toLowerCase()))
            .findFirst();

        if (existingProduit.isPresent()) {
            Produit produit = existingProduit.get();
            dto.setProduitId(produit.getIdProduit());
            dto.setNomProduit(produit.getNomProduit());
            dto.addWarning("Pack lié au produit: " + produit.getNomProduit() + " (Type: " + produit.getTypeProduit() + ")");
        } else {
            dto.addWarning("Aucun produit trouvé avec le nom: '" + nomProduit + "'. Le pack sera créé sans produit lié.");
        }
    }

    /**
     * L'association de pack à produit avec recherche floue améliorée
     */
    private void linkPackToExistingProduitWithFuzzy(ChatbotPackRequestDTO dto, String prompt, ExtractionLoggingService.ExtractionLog extractionLog) {
        // Extraire le NOM du produit spécifiquement, pas le TYPE
        String nomProduit = extractProduitNameFromPrompt(prompt);

        // Si aucun nom de produit n'est spécifié, ne pas lier automatiquement
        if (nomProduit == null || nomProduit.isBlank()) {
            dto.addWarning("Aucun produit spécifié pour la liaison. Le pack sera créé sans produit lié.");
            extractionLoggingService.logFieldExtraction(extractionLog, "produitId", 
                null, "NO_MATCH", 0.0, "NONE", "No product name specified");
            return;
        }

        // Utiliser le service de recherche floue pour trouver le meilleur match
        FuzzyProductMatcherService.MatchResult match = fuzzyProductMatcherService.findBestMatch(nomProduit);

        if (match != null && match.getSimilarity() >= 0.6) {
            Produit produit = match.getProduit();
            dto.setProduitId(produit.getIdProduit());
            dto.setNomProduit(produit.getNomProduit());
            dto.addWarning("Pack lié au produit: " + produit.getNomProduit() + " (Type: " + produit.getTypeProduit() + 
                          ", Similarité: " + String.format("%.2f", match.getSimilarity()) + ")");
            
            extractionLoggingService.logFieldExtraction(extractionLog, "produitId", 
                produit.getIdProduit(), "FUZZY_MATCH", match.getSimilarity(), match.getMatchMethod(), 
                "Matched: " + match.getMatchedTerm());
        } else {
            dto.addWarning("Aucun produit trouvé avec le nom: '" + nomProduit + "'. Le pack sera créé sans produit lié.");
            extractionLoggingService.logFieldExtraction(extractionLog, "produitId", 
                null, "NO_MATCH", 0.0, "NONE", "No match found for: " + nomProduit);
        }
    }
    
    private String extractProduitNameFromPrompt(String prompt) {
        // Chercher des expressions comme "lié au produit X", "produit X", "pour le produit X", "associé à X"
        String lowerPrompt = prompt.toLowerCase();
        // Pattern: "associé à" + nom (priorité) - plus permissif
        Pattern associePattern = Pattern.compile(
            "(?:associé à|associe à|lié à|lie à|avec|pour)\\s+([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\s]+?)(?:,|\\.|\\s+un|\\s+une|\\s+avec|\\s+le|\\s+la|\\s+les|\\s+l\\s+âge|\\s+l\\s+age|\\s+le\\s+type|\\s+un\\s+âge|\\s+un\\s+age|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = associePattern.matcher(prompt);
        if (matcher.find()) {
            String productName = matcher.group(1).trim();
            // Limiter la longueur pour éviter de capturer trop de texte
            if (productName.length() > 50) {
                productName = productName.substring(0, 50).trim();
            }
            return productName;
        }
        
        // Pattern: "produit" + nom
        Pattern produitPattern = Pattern.compile(
            "(?:lié au|pour le|avec le|du|de la)?\\s*(?:produit)\\s+([a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\s]+?)(?:,|\\.|pour|$)",
            Pattern.CASE_INSENSITIVE
        );
        matcher = produitPattern.matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
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
        data.put("domaine", promptAnalyzerService.extractDomaineMedical(prompt));
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

    private Garantie createGaranteeFromExtractedData(ComplexGuaranteeExtractionService.ExtractedGuarantee extractedGuarantee, ExtractionLoggingService.ExtractionLog extractionLog) {
        try {
            Garantie garantie = new Garantie();
            garantie.setNomGarantie(extractedGuarantee.getName());
            
            // Définir le domaine médical par défaut si non spécifié
            if (extractedGuarantee.getDomaine() != null && !extractedGuarantee.getDomaine().isBlank()) {
                garantie.setDomaine(tn.vermeg.gestionproduit.enums.DomaineMedical.fromString(extractedGuarantee.getDomaine()));
            } else {
                // Inférer le domaine à partir du nom de la garantie
                String inferredDomain = inferDomainFromGuaranteeName(extractedGuarantee.getName());
                garantie.setDomaine(tn.vermeg.gestionproduit.enums.DomaineMedical.fromString(inferredDomain));
            }
            
            // Taux de remboursement
            Double taux = extractedGuarantee.getTauxRemboursement();
            garantie.setTauxRemboursement(taux != null && taux > 0 ? taux : 0.8);
            
            // Plafond
            Double plafond = extractedGuarantee.getPlafond();
            garantie.setPlafondAnnuel(plafond != null && plafond > 0 ? plafond : 10000);
            garantie.setPlafondMensuel(plafond != null && plafond > 0 ? plafond / 12 : 1000);
            garantie.setPlafondParActe(plafond != null && plafond > 0 ? plafond / 100 : 100);
            
            // Franchise
            Double franchise = extractedGuarantee.getFranchise();
            garantie.setFranchise(franchise != null ? franchise : 0);
            
            // Type de montant
            garantie.setTypeMontant(TypeMontant.TARIF_CONVENTIONNE);
            
            // Statut actif par défaut
            garantie.setStatut(Statut.ACTIF);
            
            // Sauvegarder la garantie
            Garantie savedGarantie = garantieRepository.save(garantie);
            logger.info("Garantie créée: {} avec ID: {}", savedGarantie.getNomGarantie(), savedGarantie.getIdGarantie());
            
            return savedGarantie;
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la garantie '{}': {}", extractedGuarantee.getName(), e.getMessage(), e);
            extractionLog.addError("Erreur création garantie: " + extractedGuarantee.getName() + " - " + e.getMessage());
            return null;
        }
    }

    private String inferDomainFromGuaranteeName(String guaranteeName) {
        if (guaranteeName == null) return "GENERAL";
        
        String lowerName = guaranteeName.toLowerCase();
        
        if (lowerName.contains("gynécologie") || lowerName.contains("gynecologie") || lowerName.contains("obstétrique") || 
            lowerName.contains("maternité") || lowerName.contains("maternite") || lowerName.contains("femme")) {
            return "GYNECOLOGIE";
        } else if (lowerName.contains("hospitalisation") || lowerName.contains("hospital") || lowerName.contains("chirurgie")) {
            return "HOSPITALISATION";
        } else if (lowerName.contains("dentaire") || lowerName.contains("dent") || lowerName.contains("orthodontie")) {
            return "DENTAIRE";
        } else if (lowerName.contains("optique") || lowerName.contains("lunettes") || lowerName.contains("vue") || lowerName.contains("yeux")) {
            return "OPTIQUE";
        } else if (lowerName.contains("pharmacie") || lowerName.contains("médicament") || lowerName.contains("medicament")) {
            return "PHARMACIE";
        } else if (lowerName.contains("analyse") || lowerName.contains("biologique") || lowerName.contains("laboratoire")) {
            return "ANALYSES";
        } else if (lowerName.contains("téléconsultation") || lowerName.contains("teleconsultation") || lowerName.contains("médecin à distance")) {
            return "TELECONSULTATION";
        } else if (lowerName.contains("psychologie") || lowerName.contains("mental") || lowerName.contains("bien-être")) {
            return "PSYCHOLOGIE";
        } else if (lowerName.contains("accident") || lowerName.contains("urgences")) {
            return "URGENCES";
        } else if (lowerName.contains("prévention") || lowerName.contains("prevention") || lowerName.contains("checkup")) {
            return "PREVENTION";
        }
        
        return "GENERAL";
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

    /**
     * Construit une réponse structurée pour la création de pack avec tous les détails
     * du pack, du produit associé et des garanties.
     */
    private PackCreationResponseDTO buildStructuredPackResponse(
            Pack createdPack,
            ChatbotPackRequestDTO dto,
            ValidationService.ValidationResult validation,
            BusinessValidationService.BusinessValidationResult businessValidation,
            ExtractionLoggingService.ExtractionLog extractionLog) {

        PackCreationResponseDTO response = new PackCreationResponseDTO(true, "Pack créé avec succès");

        // Détails du pack
        response.setPackDetails(PackCreationResponseDTO.PackDetails.fromEntity(createdPack));

        // Détails du produit associé
        if (dto.getProduitId() != null && !dto.getProduitId().isBlank()) {
            produitRepository.findById(dto.getProduitId()).ifPresent(produit -> {
                response.setProductDetails(PackCreationResponseDTO.ProductDetails.fromEntity(produit));
            });
        } else if (dto.getNomProduit() != null && !dto.getNomProduit().isBlank()) {
            produitRepository.findByNomProduitContainingIgnoreCase(dto.getNomProduit())
                    .stream()
                    .findFirst()
                    .ifPresent(produit -> {
                        response.setProductDetails(PackCreationResponseDTO.ProductDetails.fromEntity(produit));
                    });
        }

        // Détails des garanties associées
        List<PackGarantie> packGaranties = packGarantieRepository.findByPackId(createdPack.getIdPack());
        if (!packGaranties.isEmpty()) {
            List<PackCreationResponseDTO.GuaranteeDetails> guaranteeDetailsList = new ArrayList<>();
            for (PackGarantie pg : packGaranties) {
                garantieRepository.findById(pg.getGarantieId()).ifPresent(garantie -> {
                    guaranteeDetailsList.add(
                        PackCreationResponseDTO.GuaranteeDetails.fromEntities(garantie, pg)
                    );
                });
            }
            response.setGuarantees(guaranteeDetailsList);
        }

        // Warnings de validation
        if (validation.getWarnings() != null) {
            response.setWarnings(validation.getWarnings());
        }
        if (businessValidation.getValidationWarnings() != null) {
            businessValidation.getValidationWarnings().forEach(response::addWarning);
        }
        if (businessValidation.getSuggestions() != null) {
            businessValidation.getSuggestions().forEach(s -> response.addWarning("Suggestion: " + s));
        }

        // Erreurs de l'extraction
        if (extractionLog.getErrors() != null && !extractionLog.getErrors().isEmpty()) {
            extractionLog.getErrors().forEach(response::addError);
        }

        // Générer la visualisation hiérarchique
        response.buildHierarchyVisualization();

        return response;
    }
}
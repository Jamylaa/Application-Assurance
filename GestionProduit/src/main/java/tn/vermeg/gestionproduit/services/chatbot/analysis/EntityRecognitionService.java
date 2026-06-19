package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Service pour la reconnaissance automatique des entités métier dans les prompts
 * Détecte les produits, packs et garanties existants avant toute création
 */
@Service
public class EntityRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(EntityRecognitionService.class);

    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;
    private final BusinessEnumLoaderService enumLoaderService;

    public EntityRecognitionService(
            ProduitRepository produitRepository,
            PackUnifiedRepository packUnifiedRepository,
            GarantieRepository garantieRepository,
            BusinessEnumLoaderService enumLoaderService) {
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
        this.enumLoaderService = enumLoaderService;
    }

    /**
     * Résultat de la reconnaissance d'entités
     */
    public static class EntityRecognitionResult {
        private List<RecognizedProduit> recognizedProduits = new ArrayList<>();
        private List<RecognizedPack> recognizedPacks = new ArrayList<>();
        private List<RecognizedGarantie> recognizedGaranties = new ArrayList<>();
        private List<String> unrecognizedTerms = new ArrayList<>();
        private double confidenceScore;

        // Getters and Setters
        public List<RecognizedProduit> getRecognizedProduits() { return recognizedProduits; }
        public void setRecognizedProduits(List<RecognizedProduit> recognizedProduits) { this.recognizedProduits = recognizedProduits; }
        
        public List<RecognizedPack> getRecognizedPacks() { return recognizedPacks; }
        public void setRecognizedPacks(List<RecognizedPack> recognizedPacks) { this.recognizedPacks = recognizedPacks; }
        
        public List<RecognizedGarantie> getRecognizedGaranties() { return recognizedGaranties; }
        public void setRecognizedGaranties(List<RecognizedGarantie> recognizedGaranties) { this.recognizedGaranties = recognizedGaranties; }
        
        public List<String> getUnrecognizedTerms() { return unrecognizedTerms; }
        public void setUnrecognizedTerms(List<String> unrecognizedTerms) { this.unrecognizedTerms = unrecognizedTerms; }
        
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    }

    public static class RecognizedProduit {
        private String id;
        private String nom;
        private double confidence;
        private String matchedText;
        private String typeProduit;

        public RecognizedProduit(String id, String nom, double confidence, String matchedText, String typeProduit) {
            this.id = id;
            this.nom = nom;
            this.confidence = confidence;
            this.matchedText = matchedText;
            this.typeProduit = typeProduit;
        }

        // Getters
        public String getId() { return id; }
        public String getNom() { return nom; }
        public double getConfidence() { return confidence; }
        public String getMatchedText() { return matchedText; }
        public String getTypeProduit() { return typeProduit; }
    }

    public static class RecognizedPack {
        private String id;
        private String nom;
        private double confidence;
        private String matchedText;
        private String produitParentId;
        private String produitParentNom;

        public RecognizedPack(String id, String nom, double confidence, String matchedText, String produitParentId, String produitParentNom) {
            this.id = id;
            this.nom = nom;
            this.confidence = confidence;
            this.matchedText = matchedText;
            this.produitParentId = produitParentId;
            this.produitParentNom = produitParentNom;
        }

        // Getters
        public String getId() { return id; }
        public String getNom() { return nom; }
        public double getConfidence() { return confidence; }
        public String getMatchedText() { return matchedText; }
        public String getProduitParentId() { return produitParentId; }
        public String getProduitParentNom() { return produitParentNom; }
    }

    public static class RecognizedGarantie {
        private String id;
        private String nom;
        private double confidence;
        private String matchedText;
        private String domaineMedical;

        public RecognizedGarantie(String id, String nom, double confidence, String matchedText, String domaineMedical) {
            this.id = id;
            this.nom = nom;
            this.confidence = confidence;
            this.matchedText = matchedText;
            this.domaineMedical = domaineMedical;
        }

        // Getters
        public String getId() { return id; }
        public String getNom() { return nom; }
        public double getConfidence() { return confidence; }
        public String getMatchedText() { return matchedText; }
        public String getDomaineMedical() { return domaineMedical; }
    }

    /**
     * Reconnaît toutes les entités dans un prompt
     */
    public EntityRecognitionResult recognizeEntities(String prompt) {
        logger.info("Reconnaissance d'entités dans le prompt: {}", prompt);
        
        EntityRecognitionResult result = new EntityRecognitionResult();
        
        // Reconnaissance des produits
        result.setRecognizedProduits(recognizeProduits(prompt));
        
        // Reconnaissance des packs
        result.setRecognizedPacks(recognizePacks(prompt));
        
        // Reconnaissance des garanties
        result.setRecognizedGaranties(recognizeGaranties(prompt));
        
        // Identification des termes non reconnus
        result.setUnrecognizedTerms(identifyUnrecognizedTerms(prompt, result));
        
        // Calcul du score de confiance global
        result.setConfidenceScore(calculateConfidenceScore(result));
        
        logger.info("Reconnaissance terminée - Produits: {}, Packs: {}, Garanties: {}, Confiance: {}%", 
            result.getRecognizedProduits().size(),
            result.getRecognizedPacks().size(),
            result.getRecognizedGaranties().size(),
            result.getConfidenceScore() * 100);
        
        return result;
    }

    /**
     * Reconnaît les produits dans le prompt
     */
    private List<RecognizedProduit> recognizeProduits(String prompt) {
        List<RecognizedProduit> recognized = new ArrayList<>();
        List<Produit> allProduits = produitRepository.findAll();
        
        String lowerPrompt = prompt.toLowerCase();
        
        for (Produit produit : allProduits) {
            String nomProduit = produit.getNomProduit().toLowerCase();
            
            // Recherche exacte
            if (lowerPrompt.contains(nomProduit)) {
                recognized.add(new RecognizedProduit(
                    produit.getIdProduit(),
                    produit.getNomProduit(),
                    1.0,
                    nomProduit,
                    produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null
                ));
            } else {
                // Recherche floue avec distance de Levenshtein
                double similarity = calculateSimilarity(lowerPrompt, nomProduit);
                if (similarity > 0.7) {
                    recognized.add(new RecognizedProduit(
                        produit.getIdProduit(),
                        produit.getNomProduit(),
                        similarity,
                        nomProduit,
                        produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null
                    ));
                }
            }
        }
        
        // Trier par confiance décroissante
        recognized.sort(Comparator.comparingDouble(RecognizedProduit::getConfidence).reversed());
        
        return recognized;
    }

    /**
     * Reconnaît les packs dans le prompt
     */
    private List<RecognizedPack> recognizePacks(String prompt) {
        List<RecognizedPack> recognized = new ArrayList<>();
        List<Pack> allPacks = packUnifiedRepository.findAll();
        
        String lowerPrompt = prompt.toLowerCase();
        
        for (Pack pack : allPacks) {
            String nomPack = pack.getNomPack().toLowerCase();
            
            // Recherche exacte
            if (lowerPrompt.contains(nomPack)) {
                String produitParentNom = pack.getNomProduit();
                recognized.add(new RecognizedPack(
                    pack.getIdPack(),
                    pack.getNomPack(),
                    1.0,
                    nomPack,
                    pack.getProduitId(),
                    produitParentNom
                ));
            } else {
                // Recherche floue
                double similarity = calculateSimilarity(lowerPrompt, nomPack);
                if (similarity > 0.7) {
                    String produitParentNom = pack.getNomProduit();
                    recognized.add(new RecognizedPack(
                        pack.getIdPack(),
                        pack.getNomPack(),
                        similarity,
                        nomPack,
                        pack.getProduitId(),
                        produitParentNom
                    ));
                }
            }
        }
        
        // Trier par confiance décroissante
        recognized.sort(Comparator.comparingDouble(RecognizedPack::getConfidence).reversed());
        
        return recognized;
    }

    /**
     * Reconnaît les garanties dans le prompt
     */
    private List<RecognizedGarantie> recognizeGaranties(String prompt) {
        List<RecognizedGarantie> recognized = new ArrayList<>();
        List<Garantie> allGaranties = garantieRepository.findAll();
        
        String lowerPrompt = prompt.toLowerCase();
        
        for (Garantie garantie : allGaranties) {
            String nomGarantie = garantie.getNomGarantie().toLowerCase();
            
            // Recherche exacte
            if (lowerPrompt.contains(nomGarantie)) {
                recognized.add(new RecognizedGarantie(
                    garantie.getIdGarantie(),
                    garantie.getNomGarantie(),
                    1.0,
                    nomGarantie,
                    garantie.getDomaine() != null ? garantie.getDomaine().name() : null
                ));
            } else {
                // Recherche floue
                double similarity = calculateSimilarity(lowerPrompt, nomGarantie);
                if (similarity > 0.7) {
                    recognized.add(new RecognizedGarantie(
                        garantie.getIdGarantie(),
                        garantie.getNomGarantie(),
                        similarity,
                        nomGarantie,
                        garantie.getDomaine() != null ? garantie.getDomaine().name() : null
                    ));
                }
            }
        }
        
        // Trier par confiance décroissante
        recognized.sort(Comparator.comparingDouble(RecognizedGarantie::getConfidence).reversed());
        
        return recognized;
    }

    /**
     * Identifie les termes non reconnus dans le prompt
     */
    private List<String> identifyUnrecognizedTerms(String prompt, EntityRecognitionResult result) {
        Set<String> recognizedTerms = new HashSet<>();
        
        // Ajouter tous les termes reconnus
        result.getRecognizedProduits().forEach(p -> recognizedTerms.add(p.getMatchedText()));
        result.getRecognizedPacks().forEach(p -> recognizedTerms.add(p.getMatchedText()));
        result.getRecognizedGaranties().forEach(g -> recognizedTerms.add(g.getMatchedText()));
        
        // Extraire les mots du prompt
        String[] words = prompt.toLowerCase().split("\\s+");
        List<String> unrecognized = new ArrayList<>();
        
        for (String word : words) {
            // Nettoyer le mot
            String cleanedWord = word.replaceAll("[^a-zA-Zàâäéèêëïîôùûüÿñç]", "");
            if (cleanedWord.length() > 3 && !recognizedTerms.contains(cleanedWord)) {
                unrecognized.add(cleanedWord);
            }
        }
        
        return unrecognized;
    }

    /**
     * Calcule le score de confiance global de la reconnaissance
     */
    private double calculateConfidenceScore(EntityRecognitionResult result) {
        int totalEntities = result.getRecognizedProduits().size() + 
                          result.getRecognizedPacks().size() + 
                          result.getRecognizedGaranties().size();
        
        if (totalEntities == 0) {
            return 0.0;
        }
        
        double totalConfidence = 0.0;
        
        totalConfidence += result.getRecognizedProduits().stream()
            .mapToDouble(RecognizedProduit::getConfidence).sum();
        totalConfidence += result.getRecognizedPacks().stream()
            .mapToDouble(RecognizedPack::getConfidence).sum();
        totalConfidence += result.getRecognizedGaranties().stream()
            .mapToDouble(RecognizedGarantie::getConfidence).sum();
        
        return totalConfidence / totalEntities;
    }

    /**
     * Calcule la similarité entre deux chaînes (algorithme de Jaccard amélioré)
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        if (text1.equals(text2)) {
            return 1.0;
        }
        
        // Si une chaîne contient l'autre
        if (text1.contains(text2) || text2.contains(text1)) {
            return 0.9;
        }
        
        // Distance de Levenshtein normalisée
        int maxLength = Math.max(text1.length(), text2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(text1, text2);
        return 1.0 - (distance / (double) maxLength);
    }

    /**
     * Calcul de la distance de Levenshtein
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[a.length()][b.length()];
    }

    /**
     * Détecte les relations métier entre les entités reconnues
     * Par exemple: associer un pack à son produit parent
     */
    public Map<String, Object> detectBusinessRelations(EntityRecognitionResult result) {
        Map<String, Object> relations = new HashMap<>();
        
        // Association packs-produits
        List<Map<String, String>> packProduitRelations = new ArrayList<>();
        for (RecognizedPack pack : result.getRecognizedPacks()) {
            if (pack.getProduitParentId() != null) {
                Map<String, String> relation = new HashMap<>();
                relation.put("packId", pack.getId());
                relation.put("packNom", pack.getNom());
                relation.put("produitId", pack.getProduitParentId());
                relation.put("produitNom", pack.getProduitParentNom());
                packProduitRelations.add(relation);
            }
        }
        relations.put("packProduitRelations", packProduitRelations);
        
        // Vérification si les packs sont associés à des produits reconnus
        List<String> packsWithRecognizedProduit = result.getRecognizedPacks().stream()
            .filter(pack -> result.getRecognizedProduits().stream()
                .anyMatch(produit -> produit.getId().equals(pack.getProduitParentId())))
            .map(RecognizedPack::getNom)
            .collect(Collectors.toList());
        relations.put("packsWithRecognizedProduit", packsWithRecognizedProduit);
        
        return relations;
    }

    /**
     * Suggère des entités existantes pour les termes non reconnus
     */
    public Map<String, List<String>> suggestExistingEntities(List<String> unrecognizedTerms) {
        Map<String, List<String>> suggestions = new HashMap<>();
        
        for (String term : unrecognizedTerms) {
            List<String> termSuggestions = new ArrayList<>();
            
            // Suggestions de produits
            termSuggestions.addAll(enumLoaderService.suggestCorrections("produits", term, 2));
            
            // Suggestions de packs
            termSuggestions.addAll(enumLoaderService.suggestCorrections("packs", term, 2));
            
            // Suggestions de garanties
            termSuggestions.addAll(enumLoaderService.suggestCorrections("garanties", term, 2));
            
            if (!termSuggestions.isEmpty()) {
                suggestions.put(term, termSuggestions.stream().distinct().limit(5).collect(Collectors.toList()));
            }
        }
        
        return suggestions;
    }
}

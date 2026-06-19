package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de recherche floue pour associer automatiquement des produits avec des noms complexes.
 * Utilise des algorithmes de similarité de chaînes pour trouver le meilleur match.
 * 
 * @author PFE Ingénieur - GestionProduit
 * @version 2.0 - Enhanced AI Analysis Engine
 */
@Service
public class FuzzyProductMatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FuzzyProductMatcherService.class);
    private static final double SIMILARITY_THRESHOLD = 0.6;

    private final ProduitRepository produitRepository;

    public FuzzyProductMatcherService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    /**
     * Résultat du matching avec score de similarité
     */
    public static class MatchResult {
        private final Produit produit;
        private final double similarity;
        private final String matchedTerm;
        private final String matchMethod;

        public MatchResult(Produit produit, double similarity, String matchedTerm, String matchMethod) {
            this.produit = produit;
            this.similarity = similarity;
            this.matchedTerm = matchedTerm;
            this.matchMethod = matchMethod;
        }

        public Produit getProduit() { return produit; }
        public double getSimilarity() { return similarity; }
        public String getMatchedTerm() { return matchedTerm; }
        public String getMatchMethod() { return matchMethod; }
    }

    /**
     * Trouve le meilleur produit correspondant avec recherche floue
     */
    public MatchResult findBestMatch(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return null;
        }

        String normalizedSearch = normalizeText(searchTerm);
        logger.debug("Recherche floue pour: '{}' (normalisé: '{}')", searchTerm, normalizedSearch);

        List<Produit> allProduits = produitRepository.findAll();
        List<MatchResult> matches = new ArrayList<>();

        // Méthode 1: Correspondance exacte (priorité maximale)
        MatchResult exactMatch = findExactMatch(normalizedSearch, allProduits);
        if (exactMatch != null) {
            logger.info("Correspondance exacte trouvée: {}", exactMatch.getProduit().getNomProduit());
            return exactMatch;
        }

        // Méthode 2: Correspondance partielle
        matches.addAll(findPartialMatches(normalizedSearch, allProduits));

        // Méthode 3: Similarité Levenshtein
        matches.addAll(findLevenshteinMatches(normalizedSearch, allProduits));

        // Méthode 4: Correspondance par mots-clés
        matches.addAll(findKeywordMatches(normalizedSearch, allProduits));

        // Méthode 5: Correspondance phonétique (approximative)
        matches.addAll(findPhoneticMatches(normalizedSearch, allProduits));

        // Sélectionner le meilleur match
        MatchResult bestMatch = matches.stream()
            .max(Comparator.comparingDouble(MatchResult::getSimilarity))
            .orElse(null);

        if (bestMatch != null && bestMatch.getSimilarity() >= SIMILARITY_THRESHOLD) {
            logger.info("Meilleure correspondance trouvée: {} (similarité: {:.2f}, méthode: {})",
                       bestMatch.getProduit().getNomProduit(), bestMatch.getSimilarity(), bestMatch.getMatchMethod());
            return bestMatch;
        }

        logger.warn("Aucune correspondance satisfaisante trouvée pour: {}", searchTerm);
        return null;
    }

    /**
     * Trouve une correspondance exacte
     */
    private MatchResult findExactMatch(String searchTerm, List<Produit> produits) {
        for (Produit produit : produits) {
            String normalizedNom = normalizeText(produit.getNomProduit());
            if (normalizedNom.equalsIgnoreCase(searchTerm)) {
                return new MatchResult(produit, 1.0, searchTerm, "EXACT");
            }
        }
        return null;
    }

    /**
     * Trouve des correspondances partielles (contient)
     */
    private List<MatchResult> findPartialMatches(String searchTerm, List<Produit> produits) {
        List<MatchResult> matches = new ArrayList<>();

        for (Produit produit : produits) {
            String normalizedNom = normalizeText(produit.getNomProduit());
            
            // Vérifier si le terme de recherche contient le nom du produit
            if (searchTerm.contains(normalizedNom)) {
                double similarity = (double) normalizedNom.length() / searchTerm.length();
                matches.add(new MatchResult(produit, similarity, normalizedNom, "PARTIAL_CONTAINS"));
            }
            // Vérifier si le nom du produit contient le terme de recherche
            else if (normalizedNom.contains(searchTerm)) {
                double similarity = (double) searchTerm.length() / normalizedNom.length();
                matches.add(new MatchResult(produit, similarity, normalizedNom, "PARTIAL_CONTAINED"));
            }
        }

        return matches;
    }

    /**
     * Trouve des correspondances avec distance de Levenshtein
     */
    private List<MatchResult> findLevenshteinMatches(String searchTerm, List<Produit> produits) {
        List<MatchResult> matches = new ArrayList<>();

        for (Produit produit : produits) {
            String normalizedNom = normalizeText(produit.getNomProduit());
            double similarity = calculateLevenshteinSimilarity(searchTerm, normalizedNom);
            
            if (similarity > SIMILARITY_THRESHOLD) {
                matches.add(new MatchResult(produit, similarity, normalizedNom, "LEVENSHTEIN"));
            }
        }

        return matches;
    }

    /**
     * Trouve des correspondances par mots-clés
     */
    private List<MatchResult> findKeywordMatches(String searchTerm, List<Produit> produits) {
        List<MatchResult> matches = new ArrayList<>();
        
        String[] searchWords = searchTerm.split("\\s+");
        Set<String> searchWordSet = Arrays.stream(searchWords)
            .map(this::normalizeText)
            .filter(w -> w.length() > 2)
            .collect(Collectors.toSet());

        for (Produit produit : produits) {
            String normalizedNom = normalizeText(produit.getNomProduit());
            String[] productWords = normalizedNom.split("\\s+");
            Set<String> productWordSet = Arrays.stream(productWords)
                .map(this::normalizeText)
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());

            // Calculer l'intersection
            Set<String> intersection = new HashSet<>(searchWordSet);
            intersection.retainAll(productWordSet);

            if (!intersection.isEmpty()) {
                // Similarité basée sur le nombre de mots communs
                double similarity = (double) intersection.size() / Math.max(searchWordSet.size(), productWordSet.size());
                matches.add(new MatchResult(produit, similarity, String.join(", ", intersection), "KEYWORD"));
            }
        }

        return matches;
    }

    /**
     * Trouve des correspondances phonétiques (approximatives)
     */
    private List<MatchResult> findPhoneticMatches(String searchTerm, List<Produit> produits) {
        List<MatchResult> matches = new ArrayList<>();

        for (Produit produit : produits) {
            String normalizedNom = normalizeText(produit.getNomProduit());
            double similarity = calculatePhoneticSimilarity(searchTerm, normalizedNom);
            
            if (similarity > SIMILARITY_THRESHOLD) {
                matches.add(new MatchResult(produit, similarity, normalizedNom, "PHONETIC"));
            }
        }

        return matches;
    }

    /**
     * Normalise le texte pour la comparaison
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
            .replaceAll("[^a-zàâäéèêëïîôöùûüÿç\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Calcule la similarité de Levenshtein (0 à 1)
     */
    private double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) return 1.0;
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Distance de Levenshtein
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Calcule la similarité phonétique (simplifiée)
     */
    private double calculatePhoneticSimilarity(String s1, String s2) {
        // Méthode simplifiée: comparer les premières lettres et la longueur
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        // Comparaison des 3 premières lettres
        int minPrefix = Math.min(3, Math.min(s1.length(), s2.length()));
        int prefixMatch = 0;
        for (int i = 0; i < minPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixMatch++;
            }
        }
        double prefixSimilarity = (double) prefixMatch / minPrefix;

        // Comparaison de la longueur
        double lengthSimilarity = 1.0 - Math.abs(s1.length() - s2.length()) / (double) Math.max(s1.length(), s2.length());

        return (prefixSimilarity * 0.7) + (lengthSimilarity * 0.3);
    }

    /**
     * Trouve plusieurs correspondances possibles (top N)
     */
    public List<MatchResult> findTopMatches(String searchTerm, int topN) {
        String normalizedSearch = normalizeText(searchTerm);
        List<Produit> allProduits = produitRepository.findAll();
        List<MatchResult> allMatches = new ArrayList<>();

        // Collecter tous les matches
        allMatches.addAll(findExactMatch(normalizedSearch, allProduits) != null 
            ? Collections.singletonList(findExactMatch(normalizedSearch, allProduits)) 
            : Collections.emptyList());
        allMatches.addAll(findPartialMatches(normalizedSearch, allProduits));
        allMatches.addAll(findLevenshteinMatches(normalizedSearch, allProduits));
        allMatches.addAll(findKeywordMatches(normalizedSearch, allProduits));
        allMatches.addAll(findPhoneticMatches(normalizedSearch, allProduits));

        // Dédupliquer et trier par similarité
        return allMatches.stream()
            .filter(m -> m.getSimilarity() >= SIMILARITY_THRESHOLD)
            .collect(Collectors.toMap(
                m -> m.getProduit().getIdProduit(),
                m -> m,
                (m1, m2) -> m1.getSimilarity() > m2.getSimilarity() ? m1 : m2
            ))
            .values()
            .stream()
            .sorted(Comparator.comparingDouble(MatchResult::getSimilarity).reversed())
            .limit(topN)
            .collect(Collectors.toList());
    }
}

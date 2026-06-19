package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour charger dynamiquement les enums et référentiels métier
 * Permet au moteur IA d'utiliser les valeurs existantes avant toute création
 */
@Service
public class BusinessEnumLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessEnumLoaderService.class);

    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;

    private Map<String, Set<String>> cachedEnums = new HashMap<>();
    private Map<String, Set<String>> cachedEntityNames = new HashMap<>();

    public BusinessEnumLoaderService(
            ProduitRepository produitRepository,
            PackUnifiedRepository packUnifiedRepository,
            GarantieRepository garantieRepository) {
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
        loadAllEnums();
        loadAllEntityNames();
    }

    /**
     * Charge tous les enums disponibles dans le système
     */
    private void loadAllEnums() {
        logger.info("Chargement des enums métier pour le moteur IA");
        
        // Domaines médicaux
        Set<String> domainesMedicaux = Arrays.stream(DomaineMedical.values())
            .map(DomaineMedical::name)
            .collect(Collectors.toSet());
        cachedEnums.put("domaineMedical", domainesMedicaux);
        
        logger.info("Enums chargés: {}", cachedEnums.keySet());
    }

    /**
     * Charge tous les noms d'entités existants depuis la base de données
     */
    private void loadAllEntityNames() {
        logger.info("Chargement des noms d'entités depuis la base de données");
        
        // Noms de produits
        Set<String> produitNames = produitRepository.findAll().stream()
            .map(p -> p.getNomProduit().toLowerCase())
            .collect(Collectors.toSet());
        cachedEntityNames.put("produits", produitNames);
        
        // Noms de packs
        Set<String> packNames = packUnifiedRepository.findAll().stream()
            .map(p -> p.getNomPack().toLowerCase())
            .collect(Collectors.toSet());
        cachedEntityNames.put("packs", packNames);
        
        // Noms de garanties
        Set<String> garantieNames = garantieRepository.findAll().stream()
            .map(g -> g.getNomGarantie().toLowerCase())
            .collect(Collectors.toSet());
        cachedEntityNames.put("garanties", garantieNames);
        
        logger.info("Entités chargées: produits={}, packs={}, garanties={}", 
            produitNames.size(), packNames.size(), garantieNames.size());
    }

    /**
     * Recharge les données depuis la base de données
     */
    public void refreshCache() {
        logger.info("Rafraîchissement du cache des entités");
        cachedEntityNames.clear();
        loadAllEntityNames();
    }

    /**
     * Vérifie si une valeur existe dans un enum spécifique
     */
    public boolean enumValueExists(String enumType, String value) {
        Set<String> values = cachedEnums.get(enumType.toLowerCase());
        return values != null && values.stream()
            .anyMatch(v -> v.equalsIgnoreCase(value));
    }

    /**
     * Vérifie si un produit existe déjà
     */
    public boolean produitExists(String nomProduit) {
        Set<String> produits = cachedEntityNames.get("produits");
        return produits != null && produits.stream()
            .anyMatch(p -> p.equalsIgnoreCase(nomProduit.toLowerCase()));
    }

    /**
     * Vérifie si un pack existe déjà
     */
    public boolean packExists(String nomPack) {
        Set<String> packs = cachedEntityNames.get("packs");
        return packs != null && packs.stream()
            .anyMatch(p -> p.equalsIgnoreCase(nomPack.toLowerCase()));
    }

    /**
     * Vérifie si une garantie existe déjà
     */
    public boolean garantieExists(String nomGarantie) {
        Set<String> garanties = cachedEntityNames.get("garanties");
        return garanties != null && garanties.stream()
            .anyMatch(g -> g.equalsIgnoreCase(nomGarantie.toLowerCase()));
    }

    /**
     * Recherche des enums correspondants à un texte
     */
    public Map<String, List<String>> findMatchingEnums(String text) {
        Map<String, List<String>> matches = new HashMap<>();
        String lowerText = text.toLowerCase();
        
        for (Map.Entry<String, Set<String>> entry : cachedEnums.entrySet()) {
            List<String> matchingValues = entry.getValue().stream()
                .filter(value -> lowerText.contains(value.toLowerCase()) || 
                               value.toLowerCase().contains(lowerText))
                .collect(Collectors.toList());
            
            if (!matchingValues.isEmpty()) {
                matches.put(entry.getKey(), matchingValues);
            }
        }
        
        return matches;
    }

    /**
     * Recherche des entités existantes correspondantes à un texte
     */
    public Map<String, List<String>> findMatchingEntities(String text) {
        Map<String, List<String>> matches = new HashMap<>();
        String lowerText = text.toLowerCase();
        
        for (Map.Entry<String, Set<String>> entry : cachedEntityNames.entrySet()) {
            List<String> matchingValues = entry.getValue().stream()
                .filter(value -> lowerText.contains(value) || 
                               value.contains(lowerText))
                .collect(Collectors.toList());
            
            if (!matchingValues.isEmpty()) {
                matches.put(entry.getKey(), matchingValues);
            }
        }
        
        return matches;
    }

    /**
     * Obtient toutes les valeurs d'un enum spécifique
     */
    public Set<String> getEnumValues(String enumType) {
        return cachedEnums.getOrDefault(enumType.toLowerCase(), Collections.emptySet());
    }

    /**
     * Obtient tous les noms d'entités d'un type spécifique
     */
    public Set<String> getEntityNames(String entityType) {
        return cachedEntityNames.getOrDefault(entityType.toLowerCase(), Collections.emptySet());
    }

    /**
     * Suggère des corrections pour une valeur mal orthographiée
     */
    public List<String> suggestCorrections(String enumType, String value, int maxDistance) {
        Set<String> values = cachedEnums.get(enumType.toLowerCase());
        if (values == null) {
            return Collections.emptyList();
        }
        
        String lowerValue = value.toLowerCase();
        return values.stream()
            .filter(v -> levenshteinDistance(lowerValue, v.toLowerCase()) <= maxDistance)
            .sorted(Comparator.comparingInt(v -> levenshteinDistance(lowerValue, v.toLowerCase())))
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * Calcul de la distance de Levenshtein entre deux chaînes
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
     * Obtient des statistiques sur les données chargées
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enumTypes", cachedEnums.size());
        stats.put("entityTypes", cachedEntityNames.size());
        
        Map<String, Integer> enumCounts = new HashMap<>();
        cachedEnums.forEach((key, value) -> enumCounts.put(key, value.size()));
        stats.put("enumCounts", enumCounts);
        
        Map<String, Integer> entityCounts = new HashMap<>();
        cachedEntityNames.forEach((key, value) -> entityCounts.put(key, value.size()));
        stats.put("entityCounts", entityCounts);
        
        return stats;
    }
}

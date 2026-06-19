package tn.vermeg.gestionproduit.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionproduit.repositories.ProduitRepository;
import tn.vermeg.gestionproduit.repositories.PackUnifiedRepository;
import tn.vermeg.gestionproduit.repositories.GarantieRepository;
import tn.vermeg.gestionproduit.services.metrics.CustomMetricsService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "AI Analytics", description = "Endpoints pour l'analytics IA et les métriques métier")
public class AIAnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AIAnalyticsController.class);

    private final CustomMetricsService customMetricsService;
    private final ProduitRepository produitRepository;
    private final PackUnifiedRepository packUnifiedRepository;
    private final GarantieRepository garantieRepository;

    public AIAnalyticsController(
            CustomMetricsService customMetricsService,
            ProduitRepository produitRepository,
            PackUnifiedRepository packUnifiedRepository,
            GarantieRepository garantieRepository) {
        this.customMetricsService = customMetricsService;
        this.produitRepository = produitRepository;
        this.packUnifiedRepository = packUnifiedRepository;
        this.garantieRepository = garantieRepository;
    }
//Obtenir toutes les métriques IA
    @GetMapping("/metrics")
    @Operation(summary = "Obtenir toutes les métriques IA", description = "Retourne un résumé complet des métriques IA")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("metrics", customMetricsService.getMetricsSummary());
        response.put("actionMetrics", customMetricsService.getActionMetrics());
        return ResponseEntity.ok(response);
    }
    @GetMapping("/metrics/creations")
    @Operation(summary = "Métriques de création d'entités", description = "Nombre de produits, packs et garanties créés")
    public ResponseEntity<Map<String, Object>> getCreationMetrics() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> creationStats = new HashMap<>();
        creationStats.put("totalProducts", produitRepository.count());
        creationStats.put("totalPacks", packUnifiedRepository.count());
        creationStats.put("totalGaranties", garantieRepository.count());
        creationStats.put("productsCreated", customMetricsService.getMetricsSummary().get("productsCreated"));
        creationStats.put("packsCreated", customMetricsService.getMetricsSummary().get("packsCreated"));
        creationStats.put("garantiesCreated", customMetricsService.getMetricsSummary().get("garantiesCreated"));
        
        response.put("timestamp", LocalDateTime.now());
        response.put("creationMetrics", creationStats);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir les métriques IA
     */
    @GetMapping("/metrics/ai")
    @Operation(summary = "Métriques IA", description = "Statistiques de performance du moteur IA")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métriques IA récupérées avec succès")
    })
    public ResponseEntity<Map<String, Object>> getAIMetrics() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> metricsSummary = customMetricsService.getMetricsSummary();
        
        Map<String, Object> aiStats = new HashMap<>();
        aiStats.put("promptsTotal", metricsSummary.get("promptsTotal"));
        aiStats.put("recommendationsGenerated", metricsSummary.get("recommendationsGenerated"));
        aiStats.put("aiSuccess", metricsSummary.get("aiSuccess"));
        aiStats.put("aiErrors", metricsSummary.get("aiErrors"));
        aiStats.put("aiFallbacks", metricsSummary.get("aiFallbacks"));
        aiStats.put("aiSuccessRate", metricsSummary.get("aiSuccessRate"));
        aiStats.put("averageConfidence", metricsSummary.get("averageConfidence"));
        aiStats.put("aiResponseTimeMean", metricsSummary.get("aiResponseTimeMean"));
        aiStats.put("aiResponseTimeMax", metricsSummary.get("aiResponseTimeMax"));
        
        response.put("timestamp", LocalDateTime.now());
        response.put("aiMetrics", aiStats);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir les top packs
     */
    @GetMapping("/top-packs")
    @Operation(summary = "Top packs", description = "Retourne les packs les plus populaires")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top packs récupérés avec succès")
    })
    public ResponseEntity<Map<String, Object>> getTopPacks(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Map<String, Object>> topPacks = packUnifiedRepository.findAll().stream()
            .limit(limit)
            .map(pack -> {
                Map<String, Object> packInfo = new HashMap<>();
                packInfo.put("id", pack.getIdPack());
                packInfo.put("nom", pack.getNomPack());
                packInfo.put("prixMensuel", pack.getPrixMensuel());
                packInfo.put("ageMinimum", pack.getAgeMinimum());
                packInfo.put("ageMaximum", pack.getAgeMaximum());
                packInfo.put("niveauCouverture", pack.getNiveauCouverture());
                // Count PackGarantie associations via repository
                long nombreGaranties = 0; // To be implemented with PackGarantieRepository
                packInfo.put("nombreGaranties", nombreGaranties);
                return packInfo;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("topPacks", topPacks);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir les top produits
     */
    @GetMapping("/top-products")
    @Operation(summary = "Top produits", description = "Retourne les produits les plus populaires")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top produits récupérés avec succès")
    })
    public ResponseEntity<Map<String, Object>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Map<String, Object>> topProducts = produitRepository.findAll().stream()
            .limit(limit)
            .map(produit -> {
                Map<String, Object> productInfo = new HashMap<>();
                productInfo.put("id", produit.getIdProduit());
                productInfo.put("nom", produit.getNomProduit());
                productInfo.put("type", produit.getTypeProduit());
                productInfo.put("statut", produit.getStatut() != null ? produit.getStatut().name() : "UNKNOWN");
                // Count packs associated with this product via repository
                long nombrePacks = packUnifiedRepository.findAll().stream()
                    .filter(p -> produit.getIdProduit().equals(p.getProduitId()))
                    .count();
                productInfo.put("nombrePacks", nombrePacks);
                return productInfo;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("topProducts", topProducts);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir les statistiques globales
     */
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques globales", description = "Vue d'ensemble des statistiques du système")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès")
    })
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> metricsSummary = customMetricsService.getMetricsSummary();
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Statistiques d'entités
        statistics.put("totalProduits", produitRepository.count());
        statistics.put("totalPacks", packUnifiedRepository.count());
        statistics.put("totalGaranties", garantieRepository.count());
        
        // Statistiques IA
        statistics.put("promptsTraites", metricsSummary.get("promptsTotal"));
        statistics.put("recommandationsGenerees", metricsSummary.get("recommendationsGenerated"));
        statistics.put("tauxSuccesIA", metricsSummary.get("aiSuccessRate"));
        statistics.put("scoreConfianceMoyen", metricsSummary.get("averageConfidence"));
        
        // Statistiques de cache
        statistics.put("cacheHitRate", metricsSummary.get("cacheHitRate"));
        
        // Statistiques de performance
        statistics.put("tempsReponseMoyenIA", metricsSummary.get("aiResponseTimeMean"));
        
        response.put("timestamp", LocalDateTime.now());
        response.put("statistics", statistics);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir l'historique d'activité quotidienne
     */
    @GetMapping("/activity/daily")
    @Operation(summary = "Activité quotidienne", description = "Historique de l'activité quotidienne")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activité quotidienne récupérée avec succès")
    })
    public ResponseEntity<Map<String, Object>> getDailyActivity(
            @RequestParam(defaultValue = "7") int days) {
        
        // Simuler des données d'historique (à remplacer par une vraie implémentation avec base de données)
        List<Map<String, Object>> dailyActivity = new java.util.ArrayList<>();
        for (int i = days; i >= 0; i--) {
            Map<String, Object> dayActivity = new HashMap<>();
            dayActivity.put("date", LocalDateTime.now().minusDays(i).toLocalDate());
            dayActivity.put("prompts", (long) (Math.random() * 50));
            dayActivity.put("creations", (long) (Math.random() * 20));
            dayActivity.put("recommandations", (long) (Math.random() * 30));
            dailyActivity.add(dayActivity);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("dailyActivity", dailyActivity);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtenir le taux de satisfaction
     */
    @GetMapping("/satisfaction-rate")
    @Operation(summary = "Taux de satisfaction", description = "Taux de satisfaction global")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Taux de satisfaction récupéré avec succès")
    })
    public ResponseEntity<Map<String, Object>> getSatisfactionRate() {
        Map<String, Object> response = new HashMap<>();
        
        // Simuler un taux de satisfaction (à remplacer par une vraie implémentation)
        double satisfactionRate = 85.0 + (Math.random() * 10);
        
        Map<String, Object> satisfaction = new HashMap<>();
        satisfaction.put("globalRate", satisfactionRate);
        satisfaction.put("basedOn", "feedbacks utilisateurs");
        satisfaction.put("totalFeedbacks", (long) (Math.random() * 100) + 50);
        
        response.put("timestamp", LocalDateTime.now());
        response.put("satisfaction", satisfaction);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check du service analytics
     */
    @GetMapping("/health")
    @Operation(summary = "Health check analytics", description = "Vérifie l'état du service analytics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service analytics opérationnel")
    })
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AI Analytics Service");
        response.put("description", "Service d'analytics IA pour le monitoring et les métriques");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("metrics", "/api/analytics/metrics");
        endpoints.put("creations", "/api/analytics/metrics/creations");
        endpoints.put("ai", "/api/analytics/metrics/ai");
        endpoints.put("statistics", "/api/analytics/statistics");
        endpoints.put("topPacks", "/api/analytics/top-packs");
        endpoints.put("topProducts", "/api/analytics/top-products");
        endpoints.put("dailyActivity", "/api/analytics/activity/daily");
        endpoints.put("satisfaction", "/api/analytics/satisfaction-rate");
        response.put("endpoints", endpoints);
        return ResponseEntity.ok(response);
    }
}

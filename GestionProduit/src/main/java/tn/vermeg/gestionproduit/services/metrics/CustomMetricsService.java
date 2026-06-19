package tn.vermeg.gestionproduit.services.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
@Service
public class CustomMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomMetricsService.class);

    private final MeterRegistry meterRegistry;

    // Compteurs pour les actions IA
    private final Counter promptCounter;
    private final Counter productCreationCounter;
    private final Counter packCreationCounter;
    private final Counter garantieCreationCounter;
    private final Counter recommendationCounter;
    private final Counter aiSuccessCounter;
    private final Counter aiErrorCounter;
    private final Counter aiFallbackCounter;

    // Timers pour les temps de réponse
    private final Timer aiResponseTimer;
    private final Timer promptAnalysisTimer;
    private final Timer entityExtractionTimer;
    private final Timer businessValidationTimer;

    // Gauges pour les valeurs actuelles
    private final AtomicLong activePrompts = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong aiConfidenceScoreSum = new AtomicLong(0);
    private final AtomicLong aiConfidenceCount = new AtomicLong(0);

    // Métriques par type d'action
    private final ConcurrentMap<String, Counter> actionCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> actionTimers = new ConcurrentHashMap<>();

    public CustomMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialisation des compteurs
        this.promptCounter = Counter.builder("ai.prompts.total")
            .description("Total number of AI prompts processed")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.productCreationCounter = Counter.builder("business.products.created")
            .description("Total number of products created")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.packCreationCounter = Counter.builder("business.packs.created")
            .description("Total number of packs created")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.garantieCreationCounter = Counter.builder("business.garanties.created")
            .description("Total number of garanties created")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.recommendationCounter = Counter.builder("business.recommendations.generated")
            .description("Total number of recommendations generated")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.aiSuccessCounter = Counter.builder("ai.processing.success")
            .description("Number of successful AI processing operations")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.aiErrorCounter = Counter.builder("ai.processing.errors")
            .description("Number of AI processing errors")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.aiFallbackCounter = Counter.builder("ai.processing.fallback")
            .description("Number of AI fallbacks to regex processing")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        // Initialisation des timers
        this.aiResponseTimer = Timer.builder("ai.response.time")
            .description("AI response time in milliseconds")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.promptAnalysisTimer = Timer.builder("ai.prompt.analysis.time")
            .description("Time taken for prompt analysis")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.entityExtractionTimer = Timer.builder("ai.entity.extraction.time")
            .description("Time taken for entity extraction")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        this.businessValidationTimer = Timer.builder("ai.business.validation.time")
            .description("Time taken for business validation")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        // Initialisation des gauges
        Gauge.builder("ai.prompts.active", activePrompts, AtomicLong::get)
            .description("Number of currently active prompts")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        Gauge.builder("cache.hits", cacheHits, AtomicLong::get)
            .description("Number of cache hits")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        Gauge.builder("cache.misses", cacheMisses, AtomicLong::get)
            .description("Number of cache misses")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        Gauge.builder("ai.confidence.score.avg", this, CustomMetricsService::getAverageConfidence)
            .description("Average AI confidence score")
            .tag("service", "gestion-produit")
            .register(meterRegistry);

        logger.info("CustomMetricsService initialized with Prometheus integration");
    }

    // Méthodes pour incrémenter les compteurs
    public void incrementPromptCounter() {
        promptCounter.increment();
    }
    public void incrementProductCreationCounter() {
        productCreationCounter.increment();
    }
    public void incrementPackCreationCounter() {
        packCreationCounter.increment();
    }
    public void incrementGarantieCreationCounter() {
        garantieCreationCounter.increment();
    }
    public void incrementRecommendationCounter() {
        recommendationCounter.increment();
    }
    public void incrementAiSuccessCounter() {
        aiSuccessCounter.increment();
    }
    public void incrementAiErrorCounter() {
        aiErrorCounter.increment();
    }
    public void incrementAiFallbackCounter() {
        aiFallbackCounter.increment();
    }
    // Méthodes pour les timers
    public void recordAiResponseTime(long milliseconds) {
        aiResponseTimer.record(milliseconds, TimeUnit.MILLISECONDS);
    }
    public Timer.Sample startPromptAnalysisTimer() {
        return Timer.start(meterRegistry);
    }
    public void stopPromptAnalysisTimer(Timer.Sample sample) {
        sample.stop(promptAnalysisTimer);
    }
    public Timer.Sample startEntityExtractionTimer() {
        return Timer.start(meterRegistry);
    }
    public void stopEntityExtractionTimer(Timer.Sample sample) {
        sample.stop(entityExtractionTimer);
    }
    public Timer.Sample startBusinessValidationTimer() {
        return Timer.start(meterRegistry);
    }
    public void stopBusinessValidationTimer(Timer.Sample sample) {
        sample.stop(businessValidationTimer);
    }

    // Méthodes pour les gauges
    public void incrementActivePrompts() {
        activePrompts.incrementAndGet();
    }
    public void decrementActivePrompts() {
        activePrompts.decrementAndGet();
    }
    public void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }
    public void incrementCacheMisses() {
        cacheMisses.incrementAndGet();
    }
    public void recordConfidenceScore(double score) {
        aiConfidenceScoreSum.addAndGet((long) (score * 100));
        aiConfidenceCount.incrementAndGet();
    }
    private double getAverageConfidence() {
        long count = aiConfidenceCount.get();
        if (count == 0) {
            return 0.0;
        }
        return aiConfidenceScoreSum.get() / (double) count / 100.0;
    }
    // Métriques par type d'action
    public void incrementActionCounter(String actionType) {
        Counter counter = actionCounters.computeIfAbsent(actionType, key -> 
            Counter.builder("ai.actions.count")
                .description("Count of actions by type")
                .tag("action", key)
                .tag("service", "gestion-produit")
                .register(meterRegistry)
        );
        counter.increment();
    }
    public Timer.Sample startActionTimer(String actionType) {
        return Timer.start(meterRegistry);
    }
    public void stopActionTimer(Timer.Sample sample, String actionType) {
        Timer timer = actionTimers.computeIfAbsent(actionType, key ->
            Timer.builder("ai.actions.time")
                .description("Time taken for actions by type")
                .tag("action", key)
                .tag("service", "gestion-produit")
                .register(meterRegistry)
        );
        sample.stop(timer);
    }
    // Méthodes de réinitialisation (pour les tests)
    public void resetCounters() {
        activePrompts.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        aiConfidenceScoreSum.set(0);
        aiConfidenceCount.set(0);
    }
    // Statistiques agrégées
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("promptsTotal", promptCounter.count());
        summary.put("productsCreated", productCreationCounter.count());
        summary.put("packsCreated", packCreationCounter.count());
        summary.put("garantiesCreated", garantieCreationCounter.count());
        summary.put("recommendationsGenerated", recommendationCounter.count());
        summary.put("aiSuccess", aiSuccessCounter.count());
        summary.put("aiErrors", aiErrorCounter.count());
        summary.put("aiFallbacks", aiFallbackCounter.count());
        summary.put("activePrompts", activePrompts.get());
        summary.put("cacheHits", cacheHits.get());
        summary.put("cacheMisses", cacheMisses.get());
        summary.put("averageConfidence", getAverageConfidence());
        
        // Calcul du taux de succès IA
        long totalAiOperations = (long) (aiSuccessCounter.count() + aiErrorCounter.count());
        double successRate = totalAiOperations > 0 ? 
            (aiSuccessCounter.count() / totalAiOperations) * 100.0 : 0.0;
        summary.put("aiSuccessRate", successRate);
        
        // Calcul du taux de cache hit
        long totalCacheOperations = cacheHits.get() + cacheMisses.get();
        double cacheHitRate = totalCacheOperations > 0 ? 
            (cacheHits.get() / (double) totalCacheOperations) * 100.0 : 0.0;
        summary.put("cacheHitRate", cacheHitRate);
        
        // Statistiques de temps
        summary.put("aiResponseTimeMean", aiResponseTimer.mean(TimeUnit.MILLISECONDS));
        summary.put("aiResponseTimeMax", aiResponseTimer.max(TimeUnit.MILLISECONDS));
        summary.put("promptAnalysisTimeMean", promptAnalysisTimer.mean(TimeUnit.MILLISECONDS));
        
        return summary;
    }
    // Métriques par action
    public Map<String, Object> getActionMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        for (Map.Entry<String, Counter> entry : actionCounters.entrySet()) {
            Map<String, Object> actionMetric = new HashMap<>();
            actionMetric.put("count", entry.getValue().count());
            Timer timer = actionTimers.get(entry.getKey());
            if (timer != null) {
                actionMetric.put("meanTime", timer.mean(TimeUnit.MILLISECONDS));
                actionMetric.put("maxTime", timer.max(TimeUnit.MILLISECONDS));
            }
            metrics.put(entry.getKey(), actionMetric);
        }
        return metrics;
    }
}
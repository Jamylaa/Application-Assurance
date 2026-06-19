# Services Package

## 📋 Description

Ce package contient tous les services métier du microservice GestionProduit. Les services implémentent la logique métier et orchestrent les opérations entre les contrôleurs et les repositories.

## 🏗️ Structure

```
services/
├── GarantieService.java          # Service de gestion des garanties
├── PackUnifiedService.java      # Service unifié de gestion des packs
├── ProduitService.java          # Service de gestion des produits
├── chatbot/                     # Services chatbot IA
│   ├── analysis/                # Services d'analyse NLP
│   │   ├── AIExtractionService.java
│   │   ├── BusinessEnumLoaderService.java
│   │   ├── ComplexGuaranteeExtractionService.java
│   │   ├── EnhancedNumericExtractionService.java
│   │   ├── EntityRecognitionService.java
│   │   ├── ExtractionLoggingService.java
│   │   ├── FuzzyProductMatcherService.java
│   │   ├── NLPNormalizationService.java
│   │   ├── PromptAnalyzerService.java
│   │   └── PromptSegmentationService.java
│   ├── orchestration/           # Services d'orchestration
│   │   ├── ActionNormalizationService.java
│   │   ├── BusinessRelationshipService.java
│   │   ├── BusinessValidationService.java
│   │   ├── ChatbotOrchestratorService.java
│   │   └── ValidationService.java
│   ├── scoring/                 # Services de scoring
│   │   ├── BusinessScoringEngine.java
│   │   └── RecommendationService.java
│   ├── rag/                     # Services RAG
│   │   ├── RAGService.java
│   │   └── VectorEmbeddingService.java
│   ├── memory/                  # Services de mémoire
│   │   └── ConversationMemoryService.java
│   └── core/                    # Services core
├── kafka/                       # Services Kafka
│   └── KafkaEventProducer.java
└── metrics/                     # Services de métriques
    └── CustomMetricsService.java
```

## 📝 Services Principaux

### Services Métier Core

#### GarantieService
- Gestion CRUD des garanties
- Validation des données garanties
- Recherche par critères (domaine, statut, taux de remboursement)
- Gestion des statuts et désactivation

#### PackUnifiedService
- Gestion CRUD des packs
- Association packs-garanties
- Association packs-produits
- Calcul des prix totaux
- Recherche et filtrage avancé

#### ProduitService
- Gestion CRUD des produits
- Validation des données produits
- Recherche par type et statut
- Gestion des statuts

### Services Chatbot

#### Analysis Services
- **AIExtractionService** : Extraction de données structurées via Gemini AI
- **PromptAnalyzerService** : Analyse des prompts utilisateur
- **PromptSegmentationService** : Segmentation des prompts en sections
- **EntityRecognitionService** : Reconnaissance d'entités métier
- **NLPNormalizationService** : Normalisation NLP des données extraites

#### Orchestration Services
- **ChatbotOrchestratorService** : Orchestrateur principal du chatbot
- **ActionNormalizationService** : Normalisation des actions métier
- **BusinessValidationService** : Validation métier des données
- **BusinessRelationshipService** : Gestion des relations métier
- **ValidationService** : Validation des données extraites

#### Scoring Services
- **BusinessScoringEngine** : Moteur de scoring métier
- **RecommendationService** : Service de recommandation

#### Memory Services
- **ConversationMemoryService** : Gestion de la mémoire conversationnelle

#### RAG Services
- **RAGService** : Service RAG (Retrieval Augmented Generation)
- **VectorEmbeddingService** : Service d'embeddings vectoriels

### Services Infrastructure

#### Kafka Services
- **KafkaEventProducer** : Producteur d'événements Kafka

#### Metrics Services
- **CustomMetricsService** : Service de métriques personnalisées

## 🔧 Bonnes Pratiques

### Création d'un Nouveau Service

1. **Annoter avec @Service**
```java
@Service
public class MonService {
    // implémentation
}
```

2. **Injection des dépendances via constructeur**
```java
private final MonRepository repository;
private final AutreService autreService;

public MonService(MonRepository repository, AutreService autreService) {
    this.repository = repository;
    this.autreService = autreService;
}
```

3. **Documentation Javadoc**

4. **Gestion des exceptions**
- Utiliser des exceptions personnalisées
- Messages d'erreur clairs et informatifs
- Logging approprié

5. **Validation**
- Valider les entrées
- Utiliser Jakarta Validation si nécessaire
- Retourner des messages d'erreur explicites

### Transactions

Utiliser `@Transactional` pour les opérations de modification de données :
```java
@Transactional
public Pack createPack(Pack pack) {
    // opération de modification
}
```

### Logging

Logger les opérations importantes :
```java
private static final Logger logger = LoggerFactory.getLogger(MonService.class);

logger.info("Message informatif");
logger.error("Message d'erreur", exception);
```

## 📊 Dépendances

Les services dépendent généralement de :
- **Repositories** : Pour l'accès aux données
- **Autres Services** : Pour la logique métier composée
- **DTOs** : Pour le transfert de données
- **Entities** : Pour la manipulation des données métier

## 🔍 Tests

Chaque service doit avoir des tests unitaires couvrant :
- Les cas nominaux
- Les cas d'erreur
- Les validations
- Les scénarios limites

---
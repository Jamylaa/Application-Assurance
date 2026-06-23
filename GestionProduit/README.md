# GestionProduit - Module de Gestion des Produits d'Assurance

## Description

Ce module fait partie du projet Vermeg et gère la gestion des produits d'assurance, des packs et des garanties. Il inclut un moteur de scoring métier avancé pour les recommandations.

**Projet de Fin d'Études d'Ingénieur - Année 2024**

## Fonctionnalités Principales

### 1. Gestion des Produits d'Assurance
- CRUD complet des produits d'assurance (Santé, Vie, Auto, Habitation)
- Gestion des packs d'assurance avec associations de garanties
- Gestion des garanties individuelles et configurations
- Système de validation robuste des données
- Gestion des statuts et des niveaux de couverture

### 2. Système de Chatbot Intelligent
Architecture de chatbot basée sur le pattern **Orchestrator** avec services spécialisés :

- **PromptAnalyzerService** : Analyse les prompts utilisateurs pour détecter les actions et intentions
- **PromptSegmentationService** : Segmente les prompts en sections (produits, garanties, packs)
- **AIExtractionService** : Extrait des données structurées à partir de prompts naturels (intégration Gemini AI)
- **ActionNormalizationService** : Normalise les actions détectées selon les règles métier
- **ValidationService** : Valide les données extraites avec des règles de validation complexes
- **ChatbotOrchestratorService** : Orchestre tous les services du chatbot avec gestion des erreurs

### 3. Moteur de Scoring Métier (BusinessScoringEngine)
Système de recommandation intelligent basé sur des critères pondérés :

#### Critères de Scoring
- **Âge (15%)** : Éligibilité selon les limites d'âge du pack
- **Budget (20%)** : Adéquation entre le prix et le budget du client
- **Bénéficiaires (15%)** : Correspondance avec le type de famille
- **Risque Médical (20%)** : Prise en compte des maladies chroniques
- **Correspondance des Garanties (20%)** : Adéquation des garanties avec les besoins
- **Niveau de Couverture (10%)** : Correspondance du niveau de couverture souhaité

#### DTOs Principaux
- **ClientProfile** : Profil client complet (démographie, santé, préférences)
- **ScoringResult** : Résultat de scoring détaillé par critère
- **RecommendationResultDTO** : Résultat de recommandation avec justifications
- **RecommendationRequestDTO** : Demande de recommandation
- **RecommendationResponseDTO** : Réponse complète avec packs et produits recommandés

### 4. Services de Mémoire Conversationnelle
- **ConversationMemoryService** : Gestion de l'historique des conversations
- Sauvegarde des profils clients
- Enregistrement des recommandations fournies
- Suivi des échanges messages
- Intégration Redis pour le cache des conversations

### 5. Services RAG (Retrieval-Augmented Generation)
- **VectorEmbeddingService** : Génération d'embeddings pour la recherche sémantique
- **RAGService** : Recherche de documents pertinents basée sur les embeddings
- *Note : Ces services sont temporairement désactivés car les dépendances Spring AI ne sont pas disponibles*

### 6. Documentation API
- **Swagger/OpenAPI 3.0** : Documentation interactive de l'API accessible via `/swagger-ui.html`
- Configuration de sécurité JWT Keycloak
- Descriptions détaillées de tous les endpoints
- Exemples de requêtes et réponses

## Architecture Technique

### Technologies Utilisées
- **Java 21** : Langage principal avec support des features modernes (Records, Pattern Matching, Sealed Classes)
- **Spring Boot 3.x** : Framework principal avec auto-configuration et embedded server
- **Spring Data MongoDB** : Persistance des données avec MongoDB
- **Spring Security avec OAuth2 / Keycloak** : Authentification et autorisation sécurisées
- **Spring Cloud Eureka** : Service Discovery pour microservices
- **Spring Kafka** : Messagerie asynchrone pour événements
- **Redis** : Cache distribué pour les conversations
- **Maven** : Gestion des dépendances et build
- **Swagger/OpenAPI 3.0** : Documentation interactive de l'API
- **Gemini AI** : Intégration IA pour l'extraction de données

### Patterns de Conception Utilisés

#### 1. Architecture en Couches (Layered Architecture)
```
Controller Layer → Service Layer → Repository Layer → Database
```
- **Controllers** : Gestion des requêtes HTTP et réponses
- **Services** : Logique métier et orchestration
- **Repositories** : Accès aux données (Spring Data MongoDB)

#### 2. Pattern DTO (Data Transfer Object)
Séparation entre les entités de persistance et les objets de transfert de données :
- Optimisation des échanges réseau
- Validation des données entrantes
- Isolation des couches

#### 3. Pattern Repository
Abstraction de l'accès aux données avec Spring Data MongoDB :
- Méthodes de query automatiques
- Pagination et tri
- Custom queries avec @Query

#### 4. Pattern Strategy (Scoring Engine)
Le moteur de scoring utilise le pattern Strategy pour les différents critères de scoring :
- Extensibilité des critères
- Configurabilité des poids
- Isolation des algorithmes de scoring

### Architecture Microservices

Le module GestionProduit est conçu comme un microservice autonome :

```
┌─────────────────────────────────────────────────┐
│          API Gateway / Load Balancer            │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│              Eureka Server                      │
│         (Service Discovery)                      │
└─────────────────┬───────────────────────────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
┌───▼────┐  ┌───▼────┐  ┌───▼────┐
│ Gestion│  │ Chatbot│  │   Auth │
│ Produit│  │ Service│  │Service │
└────────┘  └────────┘  └────────┘
```

### Structure du Projet

```
GestionProduit/
├── src/main/java/tn/vermeg/gestionproduit/
│   ├── dto/                          # Data Transfer Objects
│   │   ├── ChatbotResponseDTO.java
│   │   ├── ClientProfile.java
│   │   ├── RecommendationRequestDTO.java
│   │   ├── RecommendationResponseDTO.java
│   │   ├── RecommendationResultDTO.java
│   │   ├── ScoringResult.java
│   │   └── ...
│   ├── entities/                     # Entités JPA
│   │   ├── Pack.java
│   │   ├── Produit.java
│   │   ├── Garantie.java
│   │   └── ...
│   ├── enums/                       # Énumérations (toutes unifiées ici)
│   │   ├── DomaineMedical.java
│   │   ├── Statut.java
│   │   ├── TypeClient.java
│   │   ├── TypeProduit.java
│   │   ├── NiveauCouverture.java
│   │   ├── TypeMontant.java
│   │   ├── TypePlafond.java
│   │   └── CouvertureGeographique.java
│   ├── repositories/                # Repositories Spring Data
│   ├── services/                    # Services Métier
│   │   ├── chatbot/                # Services Chatbot
│   │   │   ├── BusinessScoringEngine.java
│   │   │   ├── ChatbotOrchestratorService.java
│   │   │   ├── PromptAnalyzerService.java
│   │   │   ├── AIExtractionService.java
│   │   │   ├── ActionNormalizationService.java
│   │   │   ├── ValidationService.java
│   │   │   ├── ConversationMemoryService.java
│   │   │   ├── RecommendationService.java
│   │   │   ├── RAGService.java
│   │   │   └── VectorEmbeddingService.java
│   │   ├── GarantieService.java
│   │   ├── PackUnifiedService.java
│   │   └── ProduitService.java
│   └── GestionProduitApplication.java
├── src/main/resources/
│   ├── application.yml              # Configuration principale
│   └── ...
└── src/test/                        # Tests
    └── java/tn/vermeg/gestionproduit/
        ├── services/
        │   └── chatbot/
        │       └── BusinessScoringEngineTest.java
        └── GestionProduitApplicationTests.java
```

## Configuration

### Variables d'Environnement

```bash
# Configuration MongoDB
MONGODB_URI=mongodb://admin:password@localhost:27017/vermeg_db?authSource=admin
MONGODB_DATABASE=vermeg_db

# Configuration Eureka
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka

# Configuration CORS
CORS_ALLOWED_ORIGINS=http://localhost:4200

# Profile Spring
SPRING_PROFILES_ACTIVE=local
```

### Profils Spring

- **local** : Environnement de développement local
- **docker** : Environnement Docker

## Installation et Démarrage

### Prérequis
- Java 21 ou supérieur
- Maven 3.6+
- MongoDB 4.4+
- Keycloak (optionnel, pour OAuth2)
- Eureka Server (optionnel, pour service discovery)

### Compilation

```bash
cd GestionProduit
.\mvnw.cmd clean compile
```

### Exécution des Tests

```bash
.\mvnw.cmd test
```

### Démarrage de l'Application

```bash
.\mvnw.cmd spring-boot:run
```

L'application sera accessible sur le port **9093**.

## Documentation API

### Swagger/OpenAPI
La documentation interactive de l'API est accessible via :
- **Swagger UI** : `http://localhost:9093/swagger-ui.html`
- **OpenAPI JSON** : `http://localhost:9093/v3/api-docs`

La documentation inclut :
- Description détaillée de chaque endpoint
- Paramètres de requête et corps de requête
- Codes de réponse possibles
- Exemples de requêtes/réponses
- Configuration de sécurité JWT Keycloak

### Endpoints Principaux

#### Produits
- `GET /api/produits` : Liste tous les produits
- `GET /api/produits/{id}` : Récupère un produit par ID
- `POST /api/produits` : Crée un nouveau produit
- `PUT /api/produits/{id}` : Met à jour un produit
- `DELETE /api/produits/{id}` : Supprime un produit

#### Packs
- `GET /api/packs` : Liste tous les packs
- `GET /api/packs/{id}` : Récupère un pack par ID
- `GET /api/packs/search?nomPack=xxx` : Recherche des packs par nom
- `GET /api/packs/statut/{statut}` : Filtre les packs par statut
- `GET /api/packs/niveau/{niveau}` : Filtre les packs par niveau de couverture
- `POST /api/packs` : Crée un nouveau pack
- `PUT /api/packs/{id}` : Met à jour un pack
- `DELETE /api/packs/{id}` : Supprime un pack
- `POST /api/packs/{packId}/garanties/{garantieId}` : Associe une garantie à un pack
- `DELETE /api/packs/{packId}/dissociate-produit` : Dissocie un pack d'un produit

#### Garanties
- `GET /api/garanties` : Liste toutes les garanties
- `GET /api/garanties/{id}` : Récupère une garantie par ID
- `POST /api/garanties` : Crée une nouvelle garantie
- `PUT /api/garanties/{id}` : Met à jour une garantie
- `DELETE /api/garanties/{id}` : Supprime une garantie

#### Recommandations
- `POST /api/recommendations/generate` : Génère des recommandations basées sur un profil client

## Sécurité

### Authentification et Autorisation
- **OAuth2 / Keycloak** : Gestion centralisée des utilisateurs et rôles
- **JWT Tokens** : Tokens JWT signés pour l'authentification stateless
- **Role-Based Access Control (RBAC)** : Contrôle d'accès basé sur les rôles

### Configuration de Sécurité
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // Configuration OAuth2 Resource Server
    // Validation des tokens JWT
    // Règles d'accès par endpoint
}
```

### CORS
Configuration CORS pour autoriser les requêtes depuis le frontend :
```yaml
cors:
  allowed-origins: http://localhost:4200
  allowed-methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
  allowed-headers: "*"
  allow-credentials: true
```

## Performance et Scalabilité

### Optimisations
- **Redis Cache** : Cache distribué pour les conversations fréquentes
- **Indexation MongoDB** : Index sur les champs fréquemment interrogés
- **Pagination** : Pagination des résultats pour les grandes listes
- **Async Processing** : Traitement asynchrone avec Spring Kafka pour les événements

### Monitoring
- **Actuator Endpoints** : Monitoring de l'application (`/actuator/health`, `/actuator/metrics`)
- **Logging Aspect** : Logging AOP pour les opérations critiques
- **Custom Metrics** : Métriques personnalisées pour le scoring

## Tests

### Tests Unitaires
Le projet inclut des tests unitaires pour :
- **BusinessScoringEngine** : Tests du moteur de scoring avec différents scénarios
- **ProduitService** : Tests des opérations CRUD sur les produits
- **GestionProduitApplicationTests** : Test de chargement du contexte Spring

### Exécution des Tests
```bash
.\mvnw.cmd test
```

### Couverture de Code
```bash
.\mvnw.cmd test jacoco:report
```

## Développement

### Ajout d'un Nouveau Service

1. Créer la classe du service dans `src/main/java/tn/vermeg/gestionproduit/services/`
2. Annoter avec `@Service` et ajouter la Javadoc complète
3. Injecter les repositories nécessaires via constructeur
4. Implémenter la logique métier avec validation des données
5. Ajouter les tests correspondants dans `src/test/java/`
6. Documenter l'API avec des annotations Swagger

### Ajout d'un Nouveau DTO

1. Créer la classe dans `src/main/java/tn/vermeg/gestionproduit/dto/`
2. Ajouter les champs nécessaires avec leurs types
3. Générer les getters/setters ou utiliser Lombok
4. Ajouter la validation Jakarta Validation si nécessaire
5. Documenter avec Javadoc

### Bonnes Pratiques

1. **Validation des Entrées** : Toujours valider les données entrantes avec Jakarta Validation
2. **Gestion des Erreurs** : Utiliser des exceptions personnalisées avec messages clairs
3. **Transaction Management** : Utiliser `@Transactional` pour les opérations de modification de données
4. **Logging** : Logger les opérations importantes pour le debugging
5. **Documentation** : Maintenir Javadoc à jour sur toutes les classes et méthodes publiques
6. **Tests** : Maintenir une couverture de tests minimale de 80%

## Documentation Supplémentaire

- **MOTEUR_SCORING_IMPLEMENTATION.md** : Documentation détaillée de l'implémentation du moteur de scoring
- **TEST_MANUEL_SCORING.md** : Guide de test manuel pour le moteur de scoring

## Notes Importantes

### Dépendances Spring AI
Les fonctionnalités d'embeddings et de RAG (VectorEmbeddingService et RAGService) sont temporairement désactivées car les dépendances Spring AI ne sont pas disponibles dans l'environnement actuel. Ces services peuvent être réactivés une fois les dépendances ajoutées au `pom.xml`.

### Avertissements Maven
Le projet génère des avertissements concernant des dépendances dupliquées dans le `pom.xml` (spring-boot-starter-oauth2-resource-server et spring-boot-starter-security). Ces avertissements n'empêchent pas la compilation mais devraient être corrigés pour une production propre.

## Support

Pour toute question ou problème, veuillez contacter l'équipe de développement Vermeg.

## Licence

Ce projet fait partie du projet Vermeg et est soumis à ses conditions de licence.

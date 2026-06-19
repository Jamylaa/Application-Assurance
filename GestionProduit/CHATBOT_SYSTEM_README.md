# Documentation du Système Chatbot - Gestion Produit

## Vue d'ensemble

Le système chatbot est une architecture modulaire qui permet aux utilisateurs d'interagir avec le système de gestion des produits d'assurance via le langage naturel. Il utilise l'intelligence artificielle (Google Gemini) pour l'extraction de données, le RAG (Retrieval-Augmented Generation) pour la recherche sémantique, et un moteur de scoring pour les recommandations personnalisées.

---

## Architecture Globale

```
src/main/java/tn/vermeg/gestionproduit/
├── controllers/
│   ├── ChatbotController.java          # API principale du chatbot
│   └── RAGController.java              # API pour RAG et embeddings
└── services/chatbot/
    ├── analysis/                       # Analyse et extraction de données
    ├── core/                           # Éléments fondamentaux
    ├── memory/                         # Gestion de la mémoire conversationnelle
    ├── orchestration/                  # Orchestration du flux de traitement
    ├── rag/                            # Retrieval-Augmented Generation
    └── scoring/                        # Moteur de recommandations
```

---

## Controllers

### ChatbotController
**Fichier** : `controllers/ChatbotController.java`

**Rôle** : Point d'entrée principal de l'API chatbot

**Fonctionnalités** :
- Endpoint `POST /api/chatbot/process` : Traite les prompts utilisateurs
- Endpoint `GET /api/chatbot/actions` : Liste les actions supportées
- Endpoint `GET /api/chatbot/health` : Vérifie l'état de santé du chatbot
- Gestion des sessions et logging détaillé des actions

**Dépendances** : `ChatbotOrchestratorService`

---

### RAGController
**Fichier** : `controllers/RAGController.java`

**Rôle** : API pour la recherche sémantique et la gestion des embeddings

**Fonctionnalités** :
- `POST /api/rag/search` : Recherche sémantique dans les documents
- `POST /api/rag/search/fallback` : Recherche avec fallback textuel
- `POST /api/rag/index` : Indexe tous les produits, packs et garanties
- `POST /api/rag/index/{documentType}/{entityId}` : Réindexe une entité spécifique
- `POST /api/rag/embedding` : Génère un embedding pour un texte

**Dépendances** : `RAGService`, `VectorEmbeddingService`

---

## Services Chatbot - Core

### ChatbotAction
**Fichier** : `services/chatbot/core/ChatbotAction.java`

**Rôle** : Énumération des actions supportées par le chatbot

**Actions définies** :
- **Création** : `GARANTIE`, `PRODUIT`, `PACK`
- **Configuration** : `CONFIGURATION_PACK`, `AJOUT_GARANTIE_PACK`
- **Consultation** : `CONSULTATION_GARANTIE`, `CONSULTATION_PRODUIT`, `CONSULTATION_PACK`
- **Modification** : `MODIFICATION_GARANTIE`, `MODIFICATION_PRODUIT`, `MODIFICATION_PACK`
- **Suppression** : `SUPPRESSION_GARANTIE`, `SUPPRESSION_PRODUIT`, `SUPPRESSION_PACK`
- **Recommandation** : `RECOMMANDATION`

**Méthodes** :
- `normalize(String actionText)` : Normalise le texte d'action en énumération
- Supporte plusieurs langues et synonymes

---

## Services Chatbot - Analysis

### PromptAnalyzerService
**Fichier** : `services/chatbot/analysis/PromptAnalyzerService.java`

**Rôle** : Analyse les prompts pour détecter les actions et extraire les données

**Fonctionnalités principales** :
- Détection d'action à 3 niveaux de confiance (phrases explicites, combinaison action+entité, entité seule)
- Extraction de noms (produit, pack, garantie)
- Extraction de valeurs numériques (taux, plafonds, durées, prix)
- Extraction de types de clients, domaines médicaux, couvertures géographiques
- Extraction de descriptions et textes libres

**Mots-clés gérés** :
- PRODUIT, GARANTIE, PACK, RECOMMANDATION
- Verbes de création, configuration, ajout, consultation, modification, suppression

**Méthodes clés** :
- `analyzeAction(String prompt)` : Détecte l'action du prompt
- `extractNomProduit()`, `extractNomPack()`, `extractNomGarantie()` : Extraction de noms
- `extractTauxRemboursement()` : Extraction des taux de remboursement
- `extractTypeClientLabels()` : Extraction des types de clients
- `extractPlafonds()` : Extraction des plafonds (annuel, mensuel, par acte)

---

### AIExtractionService
**Fichier** : `services/chatbot/analysis/AIExtractionService.java`

**Rôle** : Utilise Google Gemini AI pour l'extraction structurée de données

**Fonctionnalités** :
- Extraction de données structurées via prompts IA
- Gestion des retries avec backoff exponentiel
- Fallback vers extraction regex si l'IA échoue
- Déduction intelligente de champs manquants (niveau de couverture, type client)

**Méthodes d'extraction** :
- `extractGarantieData()` : Extrait les données de garantie
- `extractProduitData()` : Extrait les données de produit
- `extractPackData()` : Extrait les données de pack
- `extractConfigurePackData()` : Extrait les données de configuration
- `extractAddGarantieToPackData()` : Extrait les données d'ajout de garantie

**Configuration** :
- API Key : `${google.api.key}`
- Modèle : `${gemini.model:gemini-2.0-flash}`
- Timeout : 30 secondes
- Max retries : 3

---

### ComplexGuaranteeExtractionService
**Fichier** : `services/chatbot/analysis/ComplexGuaranteeExtractionService.java`

**Rôle** : Extraction avancée de garanties complexes avec structures imbriquées

**Fonctionnalités** :
- Extraction de garanties multiples dans un seul prompt
- Gestion des structures hiérarchiques (sous-garanties)
- Normalisation des noms et descriptions
- Calcul des dérivés (plafonds mensuels, par acte)

**Classe interne** : `ExtractedGuarantee`
- Contient : nom, domaine, taux, plafonds, franchise, etc.

---

### EntityRecognitionService
**Fichier** : `services/chatbot/analysis/EntityRecognitionService.java`

**Rôle** : Reconnaissance d'entités spécifiques dans les prompts

**Fonctionnalités** :
- Identification des noms de produits existants
- Identification des noms de packs existants
- Identification des noms de garanties existantes
- Matching flou pour tolérer les variations

**Utilisation** : Permet de lier les mentions dans les prompts aux entités réelles de la base de données

---

### EnhancedNumericExtractionService
**Fichier** : `services/chatbot/analysis/EnhancedNumericExtractionService.java`

**Rôle** : Extraction avancée de valeurs numériques

**Fonctionnalités** :
- Extraction de pourcentages (80%, 0.8)
- Extraction de montants monétaires (50€, $100)
- Extraction de durées (12 mois, 2 ans)
- Extraction d'âges (45 ans, 30 y/o)
- Gestion des formats multiples et des préfixes

---

### NLPNormalizationService
**Fichier** : `services/chatbot/analysis/NLPNormalizationService.java`

**Rôle** : Normalisation NLP (Natural Language Processing)

**Fonctionnalités** :
- Normalisation des textes (minuscules, accents)
- Normalisation des énumérations (produit, pack, garantie)
- Correction orthographique basique
- Expansion des abréviations

**Méthodes de normalisation** :
- `normalizeTypeProduit()` : SANTE, AUTO, HABITATION, etc.
- `normalizeDomaineMedical()` : 60+ domaines médicaux
- `normalizeCouvertureGeographique()` : INTERNATIONAL, UE, NATIONAL, etc.
- `normalizeNiveauCouverture()` : BASIC, PREMIUM, GOLD

---

### BusinessEnumLoaderService
**Fichier** : `services/chatbot/analysis/BusinessEnumLoaderService.java`

**Rôle** : Chargement et validation des énumérations métier

**Fonctionnalités** :
- Chargement des valeurs d'énumérations depuis la base de données
- Validation des valeurs extraites
- Gestion des synonymes et variations
- Cache des énumérations pour performance

---

### FuzzyProductMatcherService
**Fichier** : `services/chatbot/analysis/FuzzyProductMatcherService.java`

**Rôle** : Matching flou pour trouver des produits similaires

**Fonctionnalités** :
- Calcul de similarité entre noms (Levenshtein, Jaccard)
- Recherche de produits par similarité
- Gestion des fautes de frappe
- Scoring des correspondances

**Utilisation** : Permet de trouver le bon produit même si le nom n'est pas exact

---

### PromptSegmentationService
**Fichier** : `services/chatbot/analysis/PromptSegmentationService.java`

**Rôle** : Segmentation des prompts en unités logiques

**Fonctionnalités** :
- Découpage des prompts en phrases
- Identification des sections (action, entité, paramètres)
- Nettoyage du texte

---

### ExtractionLoggingService
**Fichier** : `services/chatbot/analysis/ExtractionLoggingService.java`

**Rôle** : Logging détaillé de l'extraction de données

**Fonctionnalités** :
- Log de chaque champ extrait avec confiance
- Tracking de la méthode d'extraction (IA vs Regex)
- Historique des extractions pour debugging
- Métriques de performance d'extraction

---

## Services Chatbot - Orchestration

### ChatbotOrchestratorService
**Fichier** : `services/chatbot/orchestration/ChatbotOrchestratorService.java`

**Rôle** : Orchestrateur principal du flux de traitement des prompts

**Architecture du flux** :
```
Prompt → Normalisation → Validation → Détection Action → Extraction Données
→ Validation Métier → Exécution Action → Réponse
```

**Fonctionnalités** :
- Normalisation du prompt (nettoyage, formatage)
- Validation du prompt (non vide, action détectable)
- Détection de l'action via PromptAnalyzerService
- Extraction des données via AIExtractionService (avec fallback regex)
- Validation métier via BusinessValidationService
- Exécution de l'action (création, configuration, recommandation)
- Gestion des erreurs et logging

**Actions implémentées** :
- `executeCreateGarantie()` : Création de garantie
- `executeCreateProduit()` : Création de produit
- `executeCreatePack()` : Création de pack
- `executeConfigurePack()` : Configuration de pack
- `executeAddGarantieToPack()` : Ajout de garantie à un pack
- `executeRecommendation()` : Génération de recommandations

**Dépendances** : Tous les services d'analyse, validation, et business

---

### ActionNormalizationService
**Fichier** : `services/chatbot/orchestration/ActionNormalizationService.java`

**Rôle** : Normalisation des actions extraites

**Fonctionnalités** :
- Conversion des textes d'action en énumérations ChatbotAction
- Gestion des synonymes et variations linguistiques
- Mapping des verbes d'action (créer, créer, ajouter, etc.)
- Validation de l'action

---

### BusinessRelationshipService
**Fichier** : `services/chatbot/orchestration/BusinessRelationshipService.java`

**Rôle** : Gestion des relations entre entités métier

**Fonctionnalités** :
- Recherche de produits par nom
- Recherche de packs par nom
- Recherche de garanties par nom
- Validation des relations (pack-produit, pack-garantie)
- Résolution des IDs depuis les noms

**Utilisation** : Permet de lier les entités extraites aux entités réelles de la base de données

---

### BusinessValidationService
**Fichier** : `services/chatbot/orchestration/BusinessValidationService.java`

**Rôle** : Validation des règles métier

**Validations effectuées** :
- Cohérence des données (âge min < âge max)
- Validité des montants (positifs)
- Validité des pourcentages (0-100)
- Validité des énumérations
- Détection des doublons via business hash
- Validation des relations entre entités

**Méthodes** :
- `validateGarantie()` : Validation des données de garantie
- `validateProduit()` : Validation des données de produit
- `validatePack()` : Validation des données de pack
- `validatePackConfiguration()` : Validation de la configuration de pack

---

### ValidationService
**Fichier** : `services/chatbot/orchestration/ValidationService.java`

**Rôle** : Service de validation générique

**Fonctionnalités** :
- Validation des DTOs (Data Transfer Objects)
- Validation des champs obligatoires
- Validation des formats de données
- Génération de messages d'erreur

**Classe interne** : `ValidationResult`
- Contient : `isValid()`, `getErrors()`, `getWarnings()`

---

## Services Chatbot - RAG (Retrieval-Augmented Generation)

### VectorEmbeddingService
**Fichier** : `services/chatbot/rag/VectorEmbeddingService.java`

**Rôle** : Service de génération d'embeddings vectoriels

**Fonctionnalités** :
- Génération d'embeddings via l'API Google Gemini
- Indexation des produits, packs et garanties dans la base de connaissances
- Conversion des entités en documents vectoriels
- Réindexation d'entités spécifiques après modification

**Processus d'indexation** :
```
Entité (Produit/Pack/Garantie) → Construction du contenu textuel
→ Génération d'embedding → Sauvegarde dans KnowledgeDocument
```

**Méthodes** :
- `indexAllKnowledge()` : Indexe toutes les entités
- `indexProduits()`, `indexPacks()`, `indexGaranties()` : Indexation par type
- `generateEmbedding(text)` : Génère un embedding pour un texte
- `reindexEntity(documentType, entityId)` : Réindexe une entité

**Configuration** :
- Activation : `${rag.embedding.enabled:true}`
- API Key : `${google.api.key}`
- Modèle : `${spring.ai.vertex.ai.embedding.model:embedding-001}`

**Utilité** : Permet la recherche sémantique dans les produits, packs et garanties

---

### RAGService
**Fichier** : `services/chatbot/rag/RAGService.java`

**Rôle** : Service de recherche RAG (Retrieval-Augmented Generation)

**Fonctionnalités** :
- Recherche sémantique dans la base de connaissances
- Calcul de similarité cosinus entre embeddings
- Filtrage par type de document (PRODUIT, PACK, GARANTIE)
- Fallback vers recherche textuelle si embedding non disponible
- Classement des résultats par pertinence

**Méthodes** :
- `search(query)` : Recherche dans tous les documents
- `search(query, documentType)` : Recherche filtrée par type
- `searchWithFallback(query)` : Recherche avec fallback textuel

**Classe interne** : `RAGSearchResult`
- Contient : documentId, title, content, score, metadata

**Utilité** : Enrichit les réponses du chatbot avec des informations pertinentes de la base de données

---

## Services Chatbot - Scoring

### BusinessScoringEngine
**Fichier** : `services/chatbot/scoring/BusinessScoringEngine.java`

**Rôle** : Moteur de scoring pour les recommandations

**Fonctionnalités** :
- Calcul de score de compatibilité entre profil client et pack
- Scoring multi-critères pondérés
- Score minimum requis pour recommandation (40/100)

**Critères de scoring** :
- **Âge** (25%) : Éligibilité selon les limites d'âge du pack
- **Budget** (20%) : Ratio prix/budget
- **Bénéficiaires** (10%) : Adéquation type client (famille/individuel)
- **Risque médical** (35%) : Pénalité selon maladies chroniques
- **Garanties** (20%) : Correspondance des garanties
- **Niveau de couverture** (10%) : Adéquation niveau demandé

**Méthodes** :
- `calculatePackScore(profile, pack)` : Score pour un pack
- `calculateProductScore(profile, produit)` : Score pour un produit

**Pondérations des maladies** :
- Graves (cancer, insuffisance, etc.) : Exclusion (score 0)
- Modérées (diabète, hypertension, etc.) : Pénalité (20-40)
- Bénignes (cholestérol, arthrite) : Pénalité légère (70)

---

### RecommendationService
**Fichier** : `services/chatbot/scoring/RecommendationService.java`

**Rôle** : Service de génération de recommandations

**Fonctionnalités** :
- Génération de recommandations de packs basées sur le profil client
- Filtrage des packs actifs uniquement
- Scoring de tous les packs via BusinessScoringEngine
- Sélection des 3 meilleurs packs
- Génération d'explications personnalisées

**Méthodes** :
- `generateRecommendations(request)` : Génère les recommandations
- `convertToClientProfile(request)` : Convertit la requête en profil client

**Profil client** :
- Âge, genre, statut marital
- Nombre d'enfants
- Budget mensuel
- Maladies chroniques
- Niveau de couverture (déduit du budget)

**Déduction du niveau de couverture** :
- Budget < 50€ : BASIC
- Budget 50-100€ : BASIC
- Budget > 100€ : PREMIUM

---

## Services Chatbot - Memory

### ConversationMemoryService
**Fichier** : `services/chatbot/memory/ConversationMemoryService.java`

**Rôle** : Gestion de la mémoire conversationnelle

**Fonctionnalités** :
- Stockage de l'historique des conversations par sessionId
- Récupération du contexte conversationnel
- Gestion de la durée de vie des sessions
- Nettoyage des sessions expirées

**Méthodes** :
- `saveMessage(sessionId, message)` : Sauvegarde un message
- `getConversationHistory(sessionId)` : Récupère l'historique
- `clearSession(sessionId)` : Nettoie une session
- `cleanupExpiredSessions()` : Nettoie les sessions expirées

**Utilité** : Permet au chatbot de maintenir le contexte sur plusieurs échanges

---

## Controllers Associés

### ChatbotController
Voir section "Controllers" ci-dessus

### RAGController
Voir section "Controllers" ci-dessus

---

## DTOs Principaux

### ChatbotRequestDTO
**Fichier** : `dto/ChatbotRequestDTO.java`

**Champs** :
- `prompt` (String, obligatoire) : Le texte du prompt utilisateur
- `sessionId` (String, optionnel) : Identifiant de session pour la mémoire

---

### ChatbotResponseDTO
**Fichier** : `dto/ChatbotResponseDTO.java`

**Champs** :
- `success` (Boolean) : Succès ou échec
- `action` (String) : Action détectée
- `message` (String) : Message de réponse
- `result` (Object) : Résultat de l'action (entité créée, etc.)
- `data` (Map) : Métadonnées (sessionId, confiance, enums reconnus)

---

### ChatbotGarantieRequestDTO
**Fichier** : `dto/ChatbotGarantieRequestDTO.java`

**Champs** :
- Données de la garantie : nom, description, type, domaine, taux, plafonds, etc.
- Métadonnées : originalPrompt, actionDetectee, confianceExtraction
- Métadonnées de doublon : businessHash, duplicate, duplicateResolution

---

### ChatbotProduitRequestDTO
**Fichier** : `dto/ChatbotProduitRequestDTO.java`

**Champs** :
- Données du produit : nom, description, typeProduit, statut
- Métadonnées : originalPrompt, actionDetectee, confianceExtraction
- Métadonnées de doublon : businessHash, duplicate

---

### ChatbotPackRequestDTO
**Fichier** : `dto/ChatbotPackRequestDTO.java`

**Champs** :
- Données du pack : nom, description, âges, typeClients, prix, couverture, etc.
- Liaison produit : produitId, nomProduit
- Garanties : garanties (noms), complexGaranties (structures détaillées)
- Métadonnées : originalPrompt, actionDetectee, confianceExtraction
- Métadonnées de doublon : businessHash, duplicate

---

## Configuration

### application.yml

**Configuration Gemini** :
```yaml
gemini:
  api-key: ${gemini.api-key:}
  model: ${gemini.model:gemini-2.0-flash}
  url: ${gemini.url:https://generativelanguage.googleapis.com/v1beta/models}
  timeout-seconds: 30
  max-retries: 3
  retry-delay-ms: 1000
  enabled: true
```

**Configuration Chatbot** :
```yaml
chatbot:
  ai-extraction-enabled: true
  fallback-on-ai-error: true
```

**Configuration RAG** :
```yaml
rag:
  embedding.enabled: true
spring:
  ai:
    vertex:
      ai:
        embedding:
          model: embedding-001
```

---

## Flux de Traitement Complet

### Exemple : Création d'un Pack

```
1. User envoie prompt → ChatbotController.processPrompt()
2. ChatbotOrchestratorService.processPrompt()
   ├─ Normalisation du prompt
   ├─ Validation du prompt
   ├─ PromptAnalyzerService.analyzeAction() → Détecte PACK
   ├─ AIExtractionService.extractPackData() → Extrait les données
   │  ├─ Appel Google Gemini
   │  └─ Fallback regex si échec
   ├─ BusinessValidationService.validatePack()
   │  ├─ Validation des champs
   │  └─ Détection doublons
   ├─ BusinessRelationshipService.findPackId() → Vérifie produit
   └─ executeCreatePack()
      ├─ Conversion en Pack entity
      ├─ Appel PackUnifiedService.createPack()
      └─ Association des garanties
3. Réponse → ChatbotResponseDTO
```

---

## Points d'Extension

### Ajouter une nouvelle action

1. Ajouter l'énumération dans `ChatbotAction`
2. Ajouter les mots-clés dans `PromptAnalyzerService`
3. Ajouter la méthode d'extraction dans `AIExtractionService`
4. Ajouter la méthode d'exécution dans `ChatbotOrchestratorService`
5. Ajouter les validations dans `BusinessValidationService`

### Ajouter un nouveau type d'entité

1. Créer le DTO correspondant
2. Ajouter l'extraction dans `AIExtractionService`
3. Ajouter l'indexation dans `VectorEmbeddingService`
4. Ajouter la validation dans `BusinessValidationService`

---

## Performances et Optimisation

### Caching
- Énumérations chargées et mises en cache par `BusinessEnumLoaderService`
- Embeddings stockés dans `KnowledgeDocument` pour recherche rapide

### Fallback
- IA → Regex → Valeurs par défaut
- Recherche sémantique → Recherche textuelle

### Logging
- Logging détaillé pour debugging
- Métriques d'extraction via `ExtractionLoggingService`
- Métriques métier via `CustomMetricsService`

---

## Sécurité

### Validation
- Validation des entrées utilisateur
- Validation des données extraites
- Validation des règles métier

### Gestion des erreurs
- Try-catch à tous les niveaux
- Messages d'erreur explicites
- Fallback gracieux en cas d'échec

---

## Dépendances Externes

### Google Gemini AI
- Extraction de données structurées
- Génération d'embeddings vectoriels
- Requiert : API Key Google

### Spring AI
- Intégration Vertex AI
- Gestion des embeddings

---

## Conclusion

Le système chatbot est une architecture complète et modulaire qui permet :
- **Compréhension du langage naturel** via Google Gemini
- **Extraction intelligente de données** avec fallback regex
- **Validation métier robuste** avec détection de doublons
- **Recherche sémantique** via RAG et embeddings
- **Recommandations personnalisées** via moteur de scoring
- **Mémoire conversationnelle** pour contexte multi-tours

Chaque service a une responsabilité unique et bien définie, facilitant la maintenance et l'extension du système.

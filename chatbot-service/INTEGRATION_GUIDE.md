# Guide d'Intégration - Chatbot FastAPI Service

Ce guide explique l'architecture et l'intégration du service FastAPI de chatbot avec l'application Spring Boot existante.

## Vue d'ensemble de l'Architecture

L'architecture actuelle sépare clairement les responsabilités entre deux microservices autonomes :

- **Spring Boot (GestionProduit)** : Source de vérité métier
  - Gestion des entités métier (Produits, Packs, Garanties)
  - Persistance des données dans MongoDB
  - Validation métier complète et finale
  - Services business CRUD, recherche, filtrage
  - API REST pour opérations sur les entités

- **FastAPI (Chatbot)** : Assistant IA conversationnel
  - Interprétation des prompts utilisateurs en langage naturel
  - Extraction intelligente des données via Google Gemini AI
  - Analyse d'intention de l'utilisateur
  - Orchestration des appels vers GestionProduit
  - Génération de recommandations personnalisées
  - Validation légère pour feedback immédiat

**Important**: Le service chatbot-service N'ACCÈDE PAS directement à MongoDB. Toutes les opérations de persistance passent par l'API de gestionproduit.

## Architecture Technique

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Angular)                        │
│                    http://localhost:4200                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (9091)                        │
│              - Routage vers les services                      │
│              - Authentification Keycloak                     │
└──────┬────────────────────────────────────────┬───────────────┘
       │                                        │
       ▼                                        ▼
┌──────────────────────┐          ┌──────────────────────────┐
│ chatbot-service      │          │ gestionproduit            │
│ (Python FastAPI)     │          │ (Java Spring Boot)        │
│ Port: 9001           │          │ Port: 9093                │
├──────────────────────┤          ├──────────────────────────┤
│ Responsabilités:     │          │ Responsabilités:          │
│ - Interpréter prompts│          │ - CRUD Produits/Packs/    │
│ - Extraction IA      │          │   Garanties               │
│ - Analyse d'intention│          │ - Validation métier       │
│ - Orchestration API  │          │ - Persistance MongoDB     │
│ - Recommandations IA │          │ - Recherche/Filtrage      │
│                      │          │ - Business Logic          │
│ Appelle:             │          │                          │
│ gestionproduit API   │◄─────────┤                          │
└──────────────────────┘          └──────────┬───────────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │   MongoDB       │
                                   │   Port: 27017   │
                                   │   Database:     │
                                   │   vermeg_db     │
                                   └─────────────────┘
```

## Étape 1 : Configuration du Service FastAPI

### 1.1 Variables d'environnement

Créez le fichier `.env` dans le dossier `chatbot-service` :

```env
# Configuration FastAPI
FASTAPI_ENV=production
FASTAPI_PORT=9001
FASTAPI_HOST=0.0.0.0

# URL du service Spring Boot (gestionproduit)
# Pour Docker: http://gestionproduit:9093/api
# Pour local development: http://localhost:9093/api
SPRING_BOOT_BASE_URL=http://gestionproduit:9093/api

# Configuration Google Gemini AI
GEMINI_API_KEY=votre_cle_api_ici
GEMINI_MODEL=gemini-2.0-flash
GEMINI_ENABLED=true
AI_EXTRACTION_ENABLED=true
FALLBACK_ON_AI_ERROR=true
```

### 1.2 Endpoints Requis de GestionProduit

Le service FastAPI appelle les endpoints suivants de gestionproduit :

```python
# Endpoints CRUD
POST /api/garanties
GET /api/garanties/search/nom/{name}
POST /api/produits
GET /api/produits/search/nom/{name}
POST /api/packs
GET /api/packs/search/nom/{name}
GET /api/packs
POST /api/packs/{packId}/garanties
```

## Étape 2 : Déploiement

### 2.1 Option A : Services séparés (Développement Local)

Lancer les deux services séparément :

```bash
# Terminal 1 - Spring Boot (gestionproduit)
cd GestionProduit
mvn spring-boot:run

# Terminal 2 - FastAPI (chatbot-service)
cd chatbot-service
python main.py
```

### 2.2 Option B : Docker Compose (Recommandé)

Utiliser docker-compose pour orchestrer les deux services :

```yaml
# docker-compose.yml
version: '3.8'

services:
  mongodb:
    image: mongo:latest
    container_name: mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
    volumes:
      - mongodb-data:/data/db
    networks:
      - vermeg-network

  gestionproduit:
    build: ./GestionProduit
    container_name: gestionproduit
    ports:
      - "9093:9093"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATA_MONGODB_URI=mongodb://admin:password@mongodb:27017/vermeg_db?authSource=admin
    depends_on:
      - mongodb
    networks:
      - vermeg-network

  chatbot-service:
    build: ./chatbot-service
    container_name: chatbot-fastapi-service
    ports:
      - "9001:9001"
    environment:
      - FASTAPI_ENV=production
      - FASTAPI_PORT=9001
      - FASTAPI_HOST=0.0.0.0
      - SPRING_BOOT_BASE_URL=http://gestionproduit:9093/api
      - GEMINI_API_KEY=${GEMINI_API_KEY}
      - GEMINI_ENABLED=true
      - AI_EXTRACTION_ENABLED=true
    depends_on:
      - gestionproduit
    networks:
      - vermeg-network

networks:
  vermeg-network:
    driver: bridge

volumes:
  mongodb-data:
```

Lancer :

```bash
docker-compose up -d
```

## Étape 3 : Tests d'Intégration

### 3.1 Test de santé

```bash
# Tester Spring Boot (gestionproduit)
curl http://localhost:9093/actuator/health

# Tester FastAPI directement
curl http://localhost:9001/api/chatbot/health
```

### 3.2 Test de création de garantie via Chatbot

```bash
curl -X POST http://localhost:9001/api/chatbot/process \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Créer une garantie hospitalisation premium avec un remboursement de 90%",
    "session_id": "test-session"
  }'
```

### 3.3 Test de recommandation

```bash
curl -X POST http://localhost:9001/api/chatbot/recommendations \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "test-session",
    "age": 45,
    "gender": "homme",
    "marital_status": "marié",
    "number_of_children": 2,
    "monthly_budget": 150
  }'
```

## Étape 4 : Monitoring

### 4.1 Logs

Spring Boot (gestionproduit) :
```bash
# Si déployé avec Docker
docker logs -f gestionproduit
```

FastAPI (chatbot-service) :
```bash
# Si déployé avec Docker
docker logs -f chatbot-service
```

### 4.2 Health Checks

Configurer des health checks dans votre orchestrateur :

```yaml
# Kubernetes liveness probe pour chatbot-service
livenessProbe:
  httpGet:
    path: /api/chatbot/health
    port: 9001
  initialDelaySeconds: 30
  periodSeconds: 10
```

## Séparation des Responsabilités

### Ce que fait le Service Chatbot (Python) :
- ✅ Interprétation des prompts en langage naturel
- ✅ Extraction intelligente des données via Google Gemini AI
- ✅ Analyse d'intention de l'utilisateur
- ✅ Orchestration des appels API vers gestionproduit
- ✅ Génération de recommandations personnalisées
- ✅ Validation légère pour feedback immédiat à l'utilisateur
- ✅ Mode fallback si l'IA est indisponible

### Ce que fait le Service GestionProduit (Java) :
- ✅ CRUD complet sur Produits, Packs, Garanties
- ✅ Validation métier complète et finale
- ✅ Persistance des données dans MongoDB
- ✅ Recherche et filtrage avancé
- ✅ Business logic et règles métier
- ✅ API REST pour opérations sur les entités

### Ce que NE fait PAS le Service Chatbot :
- ❌ Accès direct à MongoDB
- ❌ Validation métier complexe
- ❌ Persistance des données
- ❌ Recherche/filtrage d'entités

### Ce que NE fait PAS le Service GestionProduit :
- ❌ Interprétation de langage naturel
- ❌ Extraction IA
- ❌ Génération de recommandations
- ❌ Analyse d'intention

## Checklist de Déploiement

- [ ] Configurer les variables d'environnement FastAPI (.env)
- [ ] Configurer l'URL Spring Boot dans FastAPI (gestionproduit:9093/api)
- [ ] Vérifier que tous les endpoints Spring Boot sont accessibles
- [ ] Tester la communication entre les services
- [ ] Configurer CORS si nécessaire
- [ ] Déployer avec Docker Compose ou Kubernetes
- [ ] Exécuter les tests d'intégration
- [ ] Configurer le monitoring et les logs
- [ ] Vérifier la persistance des données dans MongoDB

## Support

Pour toute question sur l'intégration, consultez :
- La documentation API FastAPI : `http://localhost:9001/docs`
- Les logs des services (docker logs)
- L'architecture technique décrite dans ce guide

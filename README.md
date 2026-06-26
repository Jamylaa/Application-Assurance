# Vermeg Assurance Pro

> Plateforme de gestion de produits d'assurance — Architecture Microservices
>
> **PFE Ingénieur — Durée : 6 mois**

![Angular](https://img.shields.io/badge/Angular-18-DD0031?logo=angular)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot)
![FastAPI](https://img.shields.io/badge/FastAPI-0.104+-009688?logo=fastapi)
![Keycloak](https://img.shields.io/badge/Keycloak-26.x-4D4D4D?logo=keycloak)
![MongoDB](https://img.shields.io/badge/MongoDB-6.x-47A248?logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/Licence-MIT-blue)

---

## Table des matières

1. [Présentation générale](#1-présentation-générale)
2. [Objectifs fonctionnels](#2-objectifs-fonctionnels)
3. [Architecture globale](#3-architecture-globale)
4. [Architecture microservices](#4-architecture-microservices)
5. [Description des microservices](#5-description-des-microservices)
6. [Frontend Angular](#6-frontend-angular)
7. [Chatbot IA](#7-chatbot-ia)
8. [Technologies utilisées](#8-technologies-utilisées)
9. [Prérequis](#9-prérequis)
10. [Installation](#10-installation)
11. [Lancement avec Docker](#11-lancement-avec-docker)
12. [Variables d'environnement](#12-variables-denvironnement)
13. [Structure du projet](#13-structure-du-projet)
14. [Fonctionnalités implémentées](#14-fonctionnalités-implémentées)
15. [Fonctionnalités restantes](#15-fonctionnalités-restantes)
16. [Planning des sprints](#16-planning-des-sprints)
17. [Sécurité Keycloak](#17-sécurité-keycloak)
18. [Observabilité](#18-observabilité)
19. [Kubernetes](#19-kubernetes)
20. [Perspectives d'amélioration](#20-perspectives-damélioration)

---

## 1. Présentation générale

**Vermeg Assurance Pro** est une plateforme de gestion de produits d'assurance développée dans le cadre d'un **Projet de Fin d'Études Ingénieur** au sein de **Vermeg**. Elle permet aux gestionnaires d'assurance de créer, configurer et analyser des produits d'assurance, des packs de couverture et des garanties médicales, le tout via une interface web moderne et un assistant IA en langage naturel.

### Contexte

La gestion des produits d'assurance implique de nombreuses entités métier fortement liées (produits, packs, garanties) et des processus de configuration complexes. L'objectif du projet est d'industrialiser cette gestion en proposant :

- Une interface web professionnelle (Angular 18 + PrimeNG)
- Une API sécurisée basée sur des microservices Spring Boot
- Un assistant IA (FastAPI + GPT-4o-mini) permettant la création en langage naturel
- Une authentification et autorisation robuste via Keycloak

---

## 2. Objectifs fonctionnels

| Objectif | Description |
|----------|-------------|
| **Gestion des produits** | CRUD complet sur les produits d'assurance avec types et domaines |
| **Gestion des packs** | Création et configuration de packs avec niveaux de couverture |
| **Gestion des garanties** | Paramétrage des garanties médicales par domaine |
| **Gestion des utilisateurs** | Administration des utilisateurs via Keycloak |
| **Assistant IA** | Création d'entités métier via des prompts en langage naturel |
| **Recommandations** | Suggestions intelligentes de packs adaptés au profil client |
| **Tableau de bord** | Indicateurs clés, graphiques et statistiques en temps réel |
| **Authentification** | Sécurisation par Keycloak (OAuth2 / OIDC / JWT) |

---

## 3. Architecture globale

```
┌─────────────────────────────────────────────────────────────────────┐
│                          NAVIGATEUR WEB                             │
│                     Angular 18 — Port 4200                          │
│              (Dashboard · Produits · Packs · Garanties)             │
│                        (Assistant IA)                               │
└───────────────────────────┬─────────────────────────────────────────┘
                            │ HTTP / REST
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY                                  │
│              Spring Cloud Gateway — Port 9091                       │
│         Routage · Validation JWT (Keycloak) · Load Balancing        │
└────────┬──────────────────┬─────────────────────┬───────────────────┘
         │                  │                     │
         ▼                  ▼                     ▼
┌────────────────┐ ┌────────────────┐  ┌────────────────────────────┐
│ GestionProduit │ │  GestionUser   │  │     Chatbot IA             │
│ Spring Boot    │ │  Spring Boot   │  │   FastAPI — Port 9001      │
│ Port 9093      │ │  Port 9092     │  │   GPT-4o-mini              │
│ MongoDB        │ │  Keycloak API  │  │   (GitHub Models API)      │
└────────┬───────┘ └────────┬───────┘  └────────────┬───────────────┘
         │                  │                        │
         └──────────────────┼────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE                               │
│   MongoDB :27017 · Keycloak :9090 · Eureka :8761                    │
│   Prometheus :9080 · Grafana :3000                                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Architecture microservices

Le projet suit le **patron d'architecture microservices** avec les principes suivants :

- **Découverte de services** via Eureka Server (Netflix Service Discovery)
- **Point d'entrée unique** via l'API Gateway (Spring Cloud Gateway)
- **Authentification centralisée** via Keycloak (OAuth2 / OIDC)
- **Persistence polyglotte** : MongoDB pour les services métier
- **Conteneurisation complète** via Docker et Docker Compose

### Flux d'une requête type

```
Client Angular
    │
    │  1. Obtient un JWT depuis Keycloak (OIDC Authorization Code Flow)
    │
    ▼
API Gateway (9091)
    │
    │  2. Valide le JWT (JWKS endpoint Keycloak)
    │  3. Route la requête vers le bon service (via Eureka)
    │
    ▼
GestionProduit / GestionUser / Chatbot
    │
    │  4. Exécute la logique métier
    │  5. Retourne la réponse
    │
    ▼
API Gateway → Client Angular
```

---

## 5. Description des microservices

### 5.1 Eureka Server

| Attribut | Valeur |
|----------|--------|
| **Rôle** | Registre de services (Service Discovery) |
| **Port** | 8761 |
| **Framework** | Spring Boot + Spring Cloud Netflix Eureka |
| **Interface** | Dashboard Eureka : `http://localhost:8761` |

Tous les microservices Spring Boot s'enregistrent au démarrage auprès d'Eureka. Le Gateway utilise Eureka pour résoudre dynamiquement les adresses des services.

---

### 5.2 API Gateway

| Attribut | Valeur |
|----------|--------|
| **Rôle** | Point d'entrée unique, routage, sécurité |
| **Port** | 9091 |
| **Framework** | Spring Cloud Gateway |
| **Auth** | Validation JWT via Keycloak JWKS |

Routes configurées :

```yaml
/api/produits/**    → gestionproduit
/api/packs/**       → gestionproduit
/api/garanties/**   → gestionproduit
/api/users/**       → gestionuser
/api/chatbot/**     → chatbot-service
```

---

### 5.3 GestionProduit

| Attribut | Valeur |
|----------|--------|
| **Rôle** | CRUD Produits, Packs, Garanties |
| **Port** | 9093 |
| **Framework** | Spring Boot 3.x |
| **Base de données** | MongoDB (`vermeg_db`) |
| **Sécurité** | Keycloak JWT (rôle ADMIN) |

**Entités principales :**

| Entité | Description |
|--------|-------------|
| `Produit` | Produit d'assurance (type, domaine, description) |
| `Pack` | Pack de couverture (niveau, prix mensuel, garanties associées) |
| `Garantie` | Garantie médicale (domaine, taux de remboursement, plafond, franchise) |

**Endpoints principaux :**

```
GET    /api/produits              Liste tous les produits
POST   /api/produits              Crée un produit
GET    /api/produits/{id}         Détails d'un produit
PUT    /api/produits/{id}         Modifie un produit
DELETE /api/produits/{id}         Supprime un produit

GET    /api/packs                 Liste tous les packs
POST   /api/packs                 Crée un pack
POST   /api/packs/{id}/garanties/{gId}  Associe une garantie à un pack

GET    /api/garanties             Liste toutes les garanties
POST   /api/garanties             Crée une garantie
```

---

### 5.4 GestionUser

| Attribut | Valeur |
|----------|--------|
| **Rôle** | Gestion des utilisateurs via l'API Keycloak |
| **Port** | 9092 |
| **Framework** | Spring Boot 3.x |
| **Sécurité** | Keycloak JWT (rôle ADMIN) |

Ce service fait le pont entre l'application et l'API d'administration Keycloak pour la gestion des utilisateurs (création, listing, attribution de rôles).

---

### 5.5 Chatbot IA (FastAPI)

| Attribut | Valeur |
|----------|--------|
| **Rôle** | Assistant IA pour création d'entités en langage naturel |
| **Port** | 9001 |
| **Framework** | FastAPI (Python 3.11) |
| **Modèle IA** | GPT-4o-mini (GitHub Models API / Azure AI Inference) |
| **Fallback** | Extraction regex si IA indisponible |

**Endpoints :**

```
POST /api/chatbot/process      Traite un prompt utilisateur
GET  /api/chatbot/health       Statut du service
GET  /api/chatbot/status       Statut IA + configuration
POST /api/chatbot/reset        Réinitialise la session
GET  /api/chatbot/actions      Actions disponibles
GET  /api/chatbot/analytics    Statistiques d'utilisation
```

**Pipeline de traitement d'un prompt :**

```
Prompt utilisateur
    │
    ▼
PromptAnalyzerService    → Détection de l'intention (CREATE_PRODUIT, CREATE_PACK, ...)
    │
    ▼
AI Service               → GPT-4o-mini : extraction des entités (JSON structuré)
    │  (+ fallback regex si IA indisponible)
    ▼
BusinessValidationService → Validation métier des données extraites
    │
    ▼
OrchestratorService      → Appel API GestionProduit pour persistance
    │
    ▼
Réponse structurée au frontend (intent, confidence, data, actions)
```

---

## 6. Frontend Angular

| Attribut | Valeur |
|----------|--------|
| **Framework** | Angular 18 (standalone components) |
| **UI Library** | PrimeNG 17 |
| **Port** | 4200 |
| **Authentification** | Keycloak Angular (`^16.1.0`) |
| **Palette** | Navy `#0f4c81` + Teal `#0d7377` + Cold Grays |

### Pages principales

| Route | Composant | Description |
|-------|-----------|-------------|
| `/dashboard` | DashboardComponent | KPIs, graphiques, assistant IA |
| `/produits` | ProduitsComponent | Liste et gestion des produits |
| `/packs` | PacksComponent | Liste et gestion des packs |
| `/garanties` | GarantiesComponent | Liste et gestion des garanties |
| `/chatbot` | UnifiedChatbotComponent | Assistant IA (vocal, historique, export) |

### Architecture des composants

```
app/
├── core/
│   └── theme.service.ts          ThemeService (clair/sombre)
├── layout/
│   └── main-layout/              Layout principal (sidebar + navbar + breadcrumb)
├── pages/
│   ├── dashboard/                Tableau de bord avec KPIs et graphiques
│   ├── produits/                 CRUD Produits
│   ├── packs/                    CRUD Packs
│   ├── garanties/                CRUD Garanties
│   └── unified-chatbot/          Assistant IA
├── services/
│   ├── gestion-produit.service   API Produits/Packs/Garanties
│   ├── gestion-user.service      API Utilisateurs
│   ├── chatbot.service           API Chatbot
│   ├── translation.service       Internationalisation
│   └── conversation-storage      Historique conversations
├── shared/
│   ├── components/
│   │   ├── sidebar.component     Navigation latérale (collapsible, thème, IA)
│   │   └── breadcrumb.component  Fil d'Ariane dynamique
│   ├── directives/               AutoScroll, ClickOutside, KeyboardShortcut
│   ├── pipes/                    TranslatePipe
│   └── services/                 BreadcrumbService, ToastService
└── models/
    └── entities.model.ts         Interfaces TypeScript (Produit, Pack, Garantie)
```

### Fonctionnalités UX notables

- **Thème clair / sombre** : toggle dans la sidebar, persisté en `localStorage`, compatible PrimeNG lara
- **i18n français** : 130+ clés de traduction via `TranslatePipe` personnalisé
- **Squelettes de chargement** : `p-skeleton` PrimeNG pendant les appels API
- **Internationalisation des dates** : getter `todayFr` sans dépendance de locale Angular
- **Chatbot vocal** : Web Speech API, historique persisté en `localStorage`, export TXT
- **Palette Navy & Teal** : CSS custom properties dans `_theme-variables.scss`, dark mode complet

---

## 7. Chatbot IA

### Intentions reconnues

| Intent | Description |
|--------|-------------|
| `CREATE_PRODUIT` | Créer un produit d'assurance |
| `CREATE_GARANTIE` | Créer une garantie médicale |
| `CREATE_PACK` | Créer un pack de couverture |
| `CONFIGURE_PACK` | Associer des garanties à un pack existant |
| `UPDATE_PRODUIT/PACK/GARANTIE` | Modifier une entité existante |
| `DELETE_PRODUIT/PACK/GARANTIE` | Supprimer une entité |
| `LIST_*` | Lister des entités |
| `RECOMMENDATION` | Recommandations de packs pour un profil client |
| `HELP` | Aide contextuelle |

### Exemple de prompt et traitement

**Entrée utilisateur :**
```
"Créer une garantie hospitalisation avec 90% de remboursement
 et un plafond annuel de 5000 TND"
```

**Réponse IA (GPT-4o-mini) :**
```json
{
  "intent": "CREATE_GARANTIE",
  "confidence": 0.97,
  "data": {
    "nom": "Garantie Hospitalisation",
    "domaineMedical": "HOSPITALISATION",
    "tauxRemboursement": 0.90,
    "plafondAnnuel": 5000,
    "typeRemboursement": "FRAIS_REELS"
  }
}
```

---

## 8. Technologies utilisées

### Backend

| Technologie | Version | Usage |
|-------------|---------|-------|
| Java | 17 | Langage backend |
| Spring Boot | 3.x | Framework microservices |
| Spring Cloud Gateway | 4.x | API Gateway |
| Spring Cloud Netflix Eureka | 4.x | Service Discovery |
| Spring Data MongoDB | 3.x | ORM MongoDB |
| Spring Security + OAuth2 | 6.x | Sécurité JWT |
| MongoDB | 6 | Base de données NoSQL |
| Keycloak | 26.x | IAM / OAuth2 / OIDC |
| Lombok | 1.18+ | Réduction boilerplate |
| SpringDoc OpenAPI | 2.x | Documentation Swagger |

### Chatbot IA

| Technologie | Version | Usage |
|-------------|---------|-------|
| Python | 3.11 | Langage chatbot |
| FastAPI | 0.104+ | Framework API REST |
| Pydantic v2 | 2.x | Validation et settings |
| httpx | 0.27+ | Client HTTP async |
| OpenAI SDK | 1.x | GPT-4o-mini via GitHub Models |

### Frontend

| Technologie | Version | Usage |
|-------------|---------|-------|
| Angular | 18 | Framework SPA (standalone) |
| PrimeNG | 17 | Composants UI (Card, Chart, Tag, Skeleton, ProgressBar) |
| Bootstrap Icons | 1.11 | Icônes |
| TypeScript | 5.x | Langage |
| SCSS | — | Design system, variables thème |
| Chart.js | 4.x | Graphiques (via PrimeNG) |

### Infrastructure

| Technologie | Version | Usage |
|-------------|---------|-------|
| Docker | 24+ | Conteneurisation |
| Docker Compose | 2.x | Orchestration locale |
| Prometheus | latest | Collecte métriques |
| Grafana | latest | Dashboards monitoring |
| Kubernetes | — | Manifests K8s (évolution) |

---

## 9. Prérequis

```bash
docker --version          # >= 24.0
docker compose version    # >= 2.20
node --version            # >= 18   (développement frontend)
java --version            # >= 17   (développement backend)
python --version          # >= 3.11 (développement chatbot)
```

---

## 10. Installation

### Cloner le projet

```bash
git clone https://github.com/<organisation>/ProjtVermeg-ye5dem.git
cd ProjtVermeg-ye5dem
```

### Configurer les variables d'environnement

```bash
# Créer le fichier .env du chatbot
cp chatbot-service/.env.example chatbot-service/.env

# Éditer et renseigner au minimum :
# GITHUB_API_KEY=<votre-clé-github-models>
```

> **⚠️ Sécurité** : Ne jamais commiter les fichiers `.env` contenant des secrets.
> S'assurer que `.env` est dans `.gitignore`.

---

## 11. Lancement avec Docker

### Démarrage complet

```bash
# Construire toutes les images et démarrer
docker compose up --build -d

# Vérifier l'état des services
docker compose ps

# Suivre les logs en temps réel
docker compose logs -f
```

### Démarrage par ordre recommandé

```bash
# 1. Infrastructure de base
docker compose up -d mongodb keycloak

# 2. Attendre que Keycloak soit prêt (~30s)
sleep 30

# 3. Service discovery
docker compose up -d eureka

# 4. Gateway + services métier
docker compose up -d gateway gestionproduit gestionuser

# 5. Chatbot IA
docker compose up -d chatbot-service

# 6. Frontend
docker compose up -d frontend
```

### Accès aux interfaces

| Service | URL | Identifiants |
|---------|-----|-------------|
| Frontend Angular | http://localhost:4200 | Keycloak admin/admin |
| API Gateway | http://localhost:9091 | JWT Bearer token requis |
| Eureka Dashboard | http://localhost:8761 | — |
| Keycloak Admin | http://localhost:9090 | admin / admin |
| GestionProduit Swagger | http://localhost:9093/swagger-ui.html | — |
| GestionUser Swagger | http://localhost:9092/swagger-ui.html | — |
| Chatbot API Docs | http://localhost:9001/docs | — |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9080 | — |

### Commandes utiles

```bash
# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes (reset complet)
docker compose down -v

# Reconstruire un service spécifique
docker compose up --build -d gestionproduit

# Logs d'un service
docker compose logs -f chatbot-service

# Shell dans un conteneur
docker compose exec gestionproduit bash
```

---

## 12. Variables d'environnement

### Services Spring Boot (docker-compose.yml)

| Variable | Service | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | Tous | `docker` en production |
| `SPRING_DATA_MONGODB_URI` | GestionProduit, GestionUser | URI MongoDB |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | Gateway, services | JWKS Keycloak |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Tous (sauf Eureka) | URL Eureka |

### Chatbot IA (chatbot-service/.env)

| Variable | Obligatoire | Description |
|----------|-------------|-------------|
| `GITHUB_API_KEY` | ✅ | Clé API GitHub Models (GPT-4o-mini) |
| `GITHUB_MODEL` | ✅ | Modèle IA (ex: `gpt-4o-mini`) |
| `GITHUB_URL` | ✅ | URL Azure AI Inference |
| `SPRING_BOOT_BASE_URL` | ✅ | URL GestionProduit (ex: `http://gestionproduit:9093/api`) |
| `FASTAPI_PORT` | — | Port FastAPI (défaut : 9001) |
| `AI_EXTRACTION_ENABLED` | — | Active l'extraction IA (défaut : `true`) |
| `FALLBACK_ON_AI_ERROR` | — | Fallback regex si IA down (défaut : `true`) |
| `GITHUB_MAX_RETRIES` | — | Nombre de tentatives IA (défaut : 3) |

---

## 13. Structure du projet

```
ProjtVermeg-ye5dem/
│
├── README.md                         Documentation principale (ce fichier)
├── docker-compose.yml                Orchestration Docker complète
├── pom.xml                           POM parent Maven (multi-modules)
│
├── Eureka/                           Service Discovery (Spring Cloud Netflix)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── Gateway/                          API Gateway (Spring Cloud Gateway)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── GestionProduit/                   Microservice Produits / Packs / Garanties
│   ├── src/main/java/.../
│   │   ├── controllers/              GarantieController, PackUnifiedController, ProduitController
│   │   ├── entities/                 Garantie, Pack, Produit
│   │   ├── repositories/             Repositories MongoDB
│   │   ├── services/                 Logique métier
│   │   ├── dto/                      Data Transfer Objects
│   │   ├── exceptions/               GlobalExceptionHandler, ErrorResponse
│   │   └── config/                   SecurityConfig, SwaggerConfig
│   ├── Dockerfile
│   └── pom.xml
│
├── GestionUser/                      Microservice Utilisateurs
│   ├── src/main/java/.../
│   │   ├── controllers/
│   │   ├── services/
│   │   ├── repositories/
│   │   └── config/                   SecurityConfig, SwaggerConfig, WebConfig
│   ├── Dockerfile
│   └── pom.xml
│
├── chatbot-service/                  Assistant IA (Python FastAPI)
│   ├── app/
│   │   ├── main.py                   Entrée FastAPI + CORS + lifespan
│   │   ├── routers/                  chatbot.py, analytics.py
│   │   ├── services/                 orchestrator, ai_service, business_validation,
│   │   │                             prompt_analyzer, prompt_parser, recommandation
│   │   ├── models/                   Pydantic models (requête/réponse)
│   │   └── utils/                    logging, exceptions
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── .env                          Variables d'environnement (NE PAS COMMITER)
│   └── README.md
│
├── frontend/                         Application Angular 18
│   ├── src/
│   │   ├── app/
│   │   │   ├── pages/                dashboard, produits, packs, garanties, chatbot
│   │   │   ├── shared/               sidebar, breadcrumb, directives, pipes
│   │   │   ├── services/             API services, translation, theme, chatbot
│   │   │   ├── models/               Interfaces TypeScript
│   │   │   └── core/                 ThemeService
│   │   ├── assets/i18n/fr.json       Traductions françaises (130+ clés)
│   │   └── styles/                   Design system SCSS, _theme-variables.scss
│   ├── package.json
│   └── angular.json
│
├── architecture/                     Documentation UML et diagrammes
│   ├── README.md
│   └── 04-physical-architecture.md
│
├── prometheus/                       Configuration Prometheus (scrape configs)
├── grafana/                          Dashboards Grafana (JSON)
└── k8s/                              Manifests Kubernetes (évolution)
```

---

## 14. Fonctionnalités implémentées

### Backend ✅

- [x] **GestionProduit** : CRUD complet Produits, Packs, Garanties (MongoDB)
- [x] **GestionUser** : gestion des utilisateurs via Keycloak Admin API
- [x] **API Gateway** : routage par préfixe, validation JWT, protection des routes
- [x] **Eureka Server** : enregistrement et découverte dynamique des services
- [x] **Sécurité** : OAuth2/JWT via Keycloak, rôle ADMIN sur toutes les routes
- [x] **Gestion des exceptions** : `GlobalExceptionHandler` avec codes HTTP appropriés
- [x] **Documentation API** : Swagger/OpenAPI sur chaque service

### Chatbot IA ✅

- [x] Détection d'intention via GPT-4o-mini (GitHub Models API / Azure)
- [x] Extraction d'entités structurées (JSON) depuis du langage naturel français
- [x] Fallback extraction regex si IA indisponible (résilience)
- [x] Validation métier des données extraites (BusinessValidationService)
- [x] Orchestration : création automatique des entités via l'API GestionProduit
- [x] Recommandations de packs avec scoring de compatibilité
- [x] Retry automatique (configurable : retries, délai)
- [x] Health check + status endpoint
- [x] Documentation OpenAPI auto-générée (`/docs`, `/redoc`)
- [x] Logging structuré avec correlation IDs
- [x] Parsing robuste JSON (strip markdown, brace-counting fallback)

### Frontend ✅

- [x] Tableau de bord (KPIs, 3 graphiques PrimeNG Chart.js, assistant IA)
- [x] CRUD complet : Produits, Packs, Garanties
- [x] Chatbot IA : interface conversationnelle, historique, export TXT, dictée vocale
- [x] Authentification Keycloak (OIDC Authorization Code Flow + PKCE)
- [x] Thème clair / sombre (PrimeNG lara-light/dark + CSS custom properties)
- [x] Internationalisation française (130+ clés, `TranslatePipe` custom)
- [x] Squelettes de chargement (`p-skeleton`) pendant les appels API
- [x] Design responsive (breakpoints 1024 / 768 / 480px)
- [x] Palette Navy & Teal cohérente sur toute l'application
- [x] Accessibilité (`aria-label`, rôles ARIA, `@supports`)

### Infrastructure ✅

- [x] Docker Compose complet (7 services : MongoDB, Keycloak, Eureka, Gateway, GestionProduit, GestionUser, Chatbot)
- [x] Prometheus (métriques Spring Boot Actuator)
- [x] Grafana (dashboards de monitoring)
- [x] Manifests Kubernetes (`k8s/`)

---

## 15. Fonctionnalités restantes

### Priorité haute

- [ ] **Healthchecks Docker** sur tous les services + `depends_on: condition: service_healthy`
- [ ] **Pagination** sur les endpoints de liste (Produits, Packs, Garanties)
- [ ] **Tests unitaires backend** : JUnit 5 + Mockito, couverture > 70%
- [ ] **Tests d'intégration** : Spring Boot Test + Testcontainers MongoDB
- [ ] **CORS strict** sur le Gateway (origines explicites, pas de `*` avec credentials)
- [ ] **Secrets management** : retirer les credentials du `docker-compose.yml` et `.env`

### Priorité moyenne

- [ ] **Config Server** (Spring Cloud Config) : externalisation complète de la configuration
- [ ] **Persistance analytics chatbot** dans Redis (actuellement en mémoire)
- [ ] **CI/CD complet** (GitHub Actions : build → test → Docker build → push → deploy)
- [ ] **Notifications temps réel** (WebSocket/SSE) pour les actions de l'assistant IA
- [ ] **Export PDF/Excel** depuis les listes Produits/Packs/Garanties
- [ ] **Tests E2E frontend** (Playwright ou Cypress)

### Priorité basse

- [ ] **Migration Kubernetes complète** (Helm charts)
- [ ] **Distributed Tracing** (Micrometer Tracing + Tempo ou Zipkin)
- [ ] **Multi-tenancy** (plusieurs compagnies d'assurance)
- [ ] **LLM local** (Ollama + Llama 3) pour indépendance des APIs payantes

---

## 16. Planning des sprints

> **Méthodologie** : Agile Scrum — 12 sprints de 2 semaines — 6 mois

| # | Période | Thème principal | Livrables clés | Statut |
|---|---------|-----------------|----------------|--------|
| S01 | 06 Jan – 17 Jan | Setup & Infrastructure | Docker Compose, Eureka, CI base | ✅ DONE |
| S02 | 20 Jan – 31 Jan | Sécurité & Keycloak | Realm configuré, JWT validé, rôles ADMIN | ✅ DONE |
| S03 | 03 Fév – 14 Fév | GestionProduit — Backend | CRUD Produits/Packs/Garanties, MongoDB | ✅ DONE |
| S04 | 17 Fév – 28 Fév | GestionUser + API Gateway | Routage, users Keycloak, Gateway sécurisé | ✅ DONE |
| S05 | 03 Mar – 14 Mar | Frontend — Structure & Auth | Angular 18, Keycloak Angular, routing | ✅ DONE |
| S06 | 17 Mar – 28 Mar | Frontend — CRUD & Dashboard | Pages Produits/Packs/Garanties, Dashboard KPIs | ✅ DONE |
| S07 | 31 Mar – 11 Avr | Chatbot IA — Architecture | FastAPI, modèles Pydantic, pipeline IA | ✅ DONE |
| S08 | 14 Avr – 25 Avr | Chatbot IA — GPT-4o-mini | Intégration GitHub Models, fallback regex | ✅ DONE |
| S09 | 28 Avr – 09 Mai | Recommandations & Analytics | Scoring compatibilité, analytics endpoint | ✅ DONE |
| S10 | 12 Mai – 23 Mai | UX/UI — Refonte Navy & Teal | Design system, dark mode, PrimeNG complet | ✅ DONE |
| S11 | 26 Mai – 06 Juin | Tests, corrections, nettoyage | Bugfixes, nettoyage code, README | 🔄 EN COURS |
| S12 | 09 Juin – 20 Juin | Documentation & Soutenance | README final, rapport PFE, présentation | 🔄 EN COURS |

---

## 17. Sécurité Keycloak

### Configuration

| Paramètre | Valeur |
|-----------|--------|
| Realm | `vermeg-realm` |
| Client Angular | `vermeg-client` (Public, PKCE) |
| Clients Backend | Resource Servers (Bearer-only) |
| Flux | Authorization Code + PKCE |
| Rôles | `ADMIN`, `USER` |
| Token | JWT signé RS256 |

### Flux d'authentification

```
Navigateur
    │  1. Accède à http://localhost:4200
    ▼
Angular + Keycloak.js
    │  2. Détecte absence de token → redirige vers Keycloak
    ▼
Keycloak (http://localhost:9090)
    │  3. Authentification utilisateur (login page)
    │  4. Retourne Authorization Code vers Angular
    ▼
Angular
    │  5. Échange code → Access Token (JWT) + Refresh Token
    │  6. Stocke token en mémoire (Keycloak Angular)
    ▼
API Gateway (http://localhost:9091)
    │  7. Requête avec Bearer token dans header Authorization
    │  8. Valide signature JWT via JWKS Keycloak
    │  9. Vérifie claims (realm_access.roles contient ADMIN)
    │  10. Route vers le microservice cible
    ▼
GestionProduit / GestionUser
    │  11. Vérifie le JWT localement (même JWKS)
    │  12. Exécute la requête
```

---

## 18. Observabilité

### Stack de monitoring

```
Services Spring Boot
    │  /actuator/prometheus
    ▼
Prometheus (http://localhost:9080)
    │  Scraping toutes les 15s
    ▼
Grafana (http://localhost:3000)
    │  Dashboards : JVM, HTTP, MongoDB
```

### Métriques disponibles

- **JVM** : heap usage, GC, threads
- **HTTP** : taux de requêtes, latence p50/p95/p99, taux d'erreur
- **Spring** : datasource connections, cache hits
- **Custom** : métriques métier (nombre de produits créés, appels chatbot)

---

## 19. Kubernetes

Des manifests Kubernetes sont disponibles dans `k8s/` pour une migration future :

```bash
# Pré-requis : kubectl + cluster configuré
kubectl create namespace vermeg

# Déployer
kubectl apply -f k8s/ -n vermeg

# Vérifier
kubectl get pods -n vermeg
kubectl get services -n vermeg
```

---

## 20. Perspectives d'amélioration

### Architecture

| Amélioration | Impact | Effort |
|-------------|--------|--------|
| Circuit Breaker (Resilience4j) | Haute résilience | Moyen |
| Config Server Spring Cloud | Configuration centralisée | Faible |
| Distributed Tracing (Tempo/Zipkin) | Observabilité end-to-end | Moyen |
| Migration Kubernetes + Helm | Scalabilité production | Élevé |
| Service Mesh (Istio) | Observabilité + sécurité mTLS | Élevé |

### Chatbot IA

| Amélioration | Impact | Effort |
|-------------|--------|--------|
| Mémoire Redis (contexte conversationnel) | Continuité des échanges | Faible |
| RAG (documentation interne comme contexte) | Réponses plus précises | Moyen |
| LLM local (Ollama + Llama 3) | Indépendance + coût | Élevé |
| Fine-tuning vocabulaire assurance tunisienne | Précision domaine | Élevé |

### Qualité & DevOps

| Amélioration | Impact | Effort |
|-------------|--------|--------|
| Tests unitaires + couverture 80% | Fiabilité | Moyen |
| Tests E2E Playwright | Non-régression | Moyen |
| CI/CD GitHub Actions complet | Déploiement continu | Moyen |
| Secrets management (Vault/Docker Secrets) | Sécurité | Faible |
| Rate limiting Gateway | Protection DDoS | Faible |

---

## Licence

Ce projet est développé dans le cadre d'un **PFE Ingénieur** chez **Vermeg**.
Tous droits réservés © 2025 Vermeg.

---

*Vermeg Assurance Pro — PFE Ingénieur 6 mois*

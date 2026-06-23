# Chatbot FastAPI Service

Microservice FastAPI pour le chatbot intelligent de gestion des produits d'assurance. Ce service remplace la partie chatbot du service Spring Boot existant, en conservant toutes les fonctionnalités métier.

## Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   Frontend      │────────▶│  FastAPI        │────────▶│  Spring Boot    │
│   (Angular)     │         │  Chatbot        │         │  GestionProduit │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                   │
                                   ▼
                            ┌──────────────┐
                            │  Google AI   │
                            │   (Gemini)   │
                            └──────────────┘
```

## Fonctionnalités

### 1. Création intelligente d'entités
- **Garanties** : Extraction automatique des données depuis un prompt en langage naturel
- **Produits** : Création de produits d'assurance avec validation
- **Packs** : Création de packs avec association automatique aux produits et garanties

### 2. Recommandation personnalisée
- Analyse du profil client (âge, situation familiale, budget, besoins médicaux)
- Recommandation des 3 meilleurs packs et produits
- Scoring basé sur multiple critères (âge, budget, couverture, géographie)

### 3. Validation métier
- Validation des données extraites
- Calcul de scores de confiance
- Suggestions d'amélioration

## Installation

### Prérequis
- Python 3.11+
- pip
- Google Gemini API Key

### Configuration locale

1. Cloner le repository :
```bash
cd chatbot-service
```

2. Créer un environnement virtuel :
```bash
python -m venv venv
source venv/bin/activate  # Sur Windows: venv\Scripts\activate
```

3. Installer les dépendances :
```bash
pip install -r requirements.txt
```

4. Configurer les variables d'environnement :
```bash
cp .env.example .env
# Éditer .env et ajouter votre clé API Gemini
```

5. Lancer le service :
```bash
python main.py
```

Le service sera accessible sur `http://localhost:8001`

## Configuration

### Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `FASTAPI_ENV` | Environnement (development/production) | `development` |
| `FASTAPI_PORT` | Port du service | `8001` |
| `FASTAPI_HOST` | Hôte du service | `0.0.0.0` |
| `SPRING_BOOT_BASE_URL` | URL du service Spring Boot | `http://localhost:8080/api` |
| `GEMINI_API_KEY` | Clé API Google Gemini | - |
| `GEMINI_MODEL` | Modèle Gemini à utiliser | `gemini-2.0-flash` |
| `GEMINI_ENABLED` | Activer le service IA | `true` |
| `AI_EXTRACTION_ENABLED` | Activer l'extraction IA | `true` |
| `FALLBACK_ON_AI_ERROR` | Utiliser le fallback si erreur IA | `true` |

## API Endpoints

### POST /api/chatbot/process
Traite un prompt en langage naturel et exécute l'action appropriée.

**Requête :**
```json
{
  "prompt": "Créer une garantie hospitalisation premium avec un remboursement de 90%",
  "session_id": "optional-session-id"
}
```

**Réponse :**
```json
{
  "success": true,
  "message": "Garantie créée avec succès",
  "action": "GARANTIE",
  "result": {
    "id": "garantie-id",
    "entity": { ... }
  }
}
```

### POST /api/chatbot/recommendations
Génère des recommandations personnalisées basées sur le profil client.

**Requête :**
```json
{
  "session_id": "session-123",
  "age": 45,
  "gender": "homme",
  "marital_status": "marié",
  "number_of_children": 2,
  "monthly_budget": 150,
  "chronic_diseases": ["diabète"],
  "geographical_zone": "NATIONAL"
}
```

**Réponse :**
```json
{
  "session_id": "session-123",
  "success": true,
  "message": "Recommandations générées avec succès",
  "explanation": "Basé sur votre profil de 45 ans, avec conditions médicales, voici les packs les plus adaptés.",
  "recommended_packs": [
    {
      "id": "pack-1",
      "nom": "Pack Santé Premium",
      "compatibility_score": 0.92,
      "why_recommended": "âge compatible, budget adapté, couverture excellente",
      "monthly_price": 120
    }
  ]
}
```

### GET /api/chatbot/health
Vérifie l'état du service.

### GET /api/chatbot/actions
Liste les actions supportées avec exemples.

### GET /api/chatbot/ai-status
Vérifie la disponibilité du service IA.

## Intégration avec Spring Boot

### Communication

Le service FastAPI communique avec le service Spring Boot via HTTP REST :

```python
# Exemple de communication
spring_boot_client = SpringBootClient()
pack = spring_boot_client.get_all_packs_sync()
garantie = spring_boot_client.create_garantie_sync(garantie_dto)
```

### Endpoints Spring Boot requis

Le service Spring Boot doit exposer les endpoints suivants :

- `GET /api/packs` - Récupérer tous les packs
- `GET /api/packs/search/nom/{name}` - Rechercher un pack par nom
- `POST /api/packs` - Créer un pack
- `GET /api/garanties/search/nom/{name}` - Rechercher une garantie par nom
- `POST /api/garanties` - Créer une garantie
- `GET /api/produits/search/nom/{name}` - Rechercher un produit par nom
- `POST /api/produits` - Créer un produit
- `POST /api/packs/{packId}/garanties` - Ajouter une garantie à un pack

## Déploiement

### Docker

Construire l'image :
```bash
docker build -t chatbot-fastapi-service .
```

Lancer le conteneur :
```bash
docker run -d -p 8001:8001 --env-file .env chatbot-fastapi-service
```

### Docker Compose

```bash
docker-compose up -d
```

### Kubernetes

Le déploiement Kubernetes peut être configuré dans le dossier `k8s/`.

```bash
kubectl apply -f k8s/
```

## Structure du projet

```
chatbot-service/
├── app/
│   ├── api/
│   │   └── chatbot.py          # Endpoints API
│   ├── models/
│   │   ├── enums.py            # Énumérations
│   │   └── schemas.py          # Models Pydantic
│   ├── services/
│   │   ├── ai_extraction_service.py      # Service d'extraction IA
│   │   ├── business_validation_service.py # Service de validation
│   │   ├── recommendation_service.py      # Service de recommandation
│   │   ├── spring_boot_client.py         # Client Spring Boot
│   │   ├── prompt_analyzer_service.py    # Analyse de prompts
│   │   └── orchestrator_service.py       # Orchestrateur
│   ├── utils/
│   │   ├── exceptions.py        # Exceptions personnalisées
│   │   └── logging.py           # Configuration logging
│   └── config.py                # Configuration
├── main.py                      # Point d'entrée
├── requirements.txt             # Dépendances Python
├── Dockerfile                   # Configuration Docker
├── docker-compose.yml           # Docker Compose
└── README.md                    # Documentation
```

## Tests

### Tests unitaires

```bash
pytest tests/
```

### Tests d'intégration

```bash
pytest tests/integration/
```

## Monitoring

### Logs

Les logs sont configurés pour sortir sur stdout et peuvent être visualisés avec :

```bash
docker logs -f chatbot-fastapi-service
```

### Health Check

```bash
curl http://localhost:8001/api/chatbot/health
```

### Métriques

L'API FastAPI expose automatiquement des métriques via `/docs` et `/redoc`.

## Bonnes pratiques

### Extensibilité

Pour ajouter un nouveau type de garantie ou de produit :

1. Ajouter l'énumération dans `app/models/enums.py`
2. Ajouter le modèle Pydantic dans `app/models/schemas.py`
3. Mettre à jour le service d'extraction IA pour reconnaître le nouveau type
4. Ajouter la logique de validation dans `business_validation_service.py`

### Performance

- Le service utilise des connexions HTTP asynchrones avec httpx
- Le cache peut être activé pour les requêtes fréquentes vers Spring Boot
- Le service IA a un système de retry automatique

### Sécurité

- Les clés API sont stockées dans les variables d'environnement
- CORS est configuré pour les origines autorisées
- Les requêtes sont validées avec Pydantic

## Dépannage

### Problème : Service IA indisponible

**Solution :**
- Vérifier que `GEMINI_API_KEY` est correctement configurée
- Vérifier que `GEMINI_ENABLED=true`
- Le service passera automatiquement en mode fallback

### Problème : Communication avec Spring Boot échoue

**Solution :**
- Vérifier que `SPRING_BOOT_BASE_URL` est correct
- Vérifier que le service Spring Boot est accessible
- Consulter les logs pour plus de détails

### Problème : Extraction incorrecte

**Solution :**
- Le service utilise un fallback regex si l'IA échoue
- Vérifier les logs pour voir la méthode d'extraction utilisée
- Ajuster les prompts dans `ai_extraction_service.py`

## Support

Pour toute question ou problème, contactez l'équipe de développement.

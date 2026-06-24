# Vermeg Assurance - Application de Gestion de Produits

Application microservices pour la gestion de produits d'assurance avec assistant IA intégré.

## 🏗️ Architecture

L'application est basée sur une architecture microservices simplifiée:

- **Frontend** (Angular, port 4200): Interface utilisateur
- **Chatbot Service** (Python FastAPI, port 9001): Assistant IA avec Google Gemini
- **GestionProduit** (Java Spring Boot, port 9093): Service de gestion des produits
- **MongoDB** (port 27017): Base de données

## 📁 Structure du Projet

```
ProjtVermeg-ye5dem/
├── frontend/              # Application Angular
├── chatbot-service/       # Service Python FastAPI
├── GestionProduit/        # Service Java Spring Boot
├── architecture/          # Documentation technique
├── k8s/                   # Configuration Kubernetes
└── docker-compose.yml     # Orchestration Docker
```

## 🚀 Démarrage Rapide

### Prérequis

- Docker et Docker Compose
- Node.js 18+ (pour le frontend en développement)
- Python 3.11+ (pour le chatbot-service en développement)
- Java 17+ (pour GestionProduit en développement)

### Lancement avec Docker Compose

```bash
# Clonez le repository
git clone <repository-url>
cd ProjtVermeg-ye5dem

# Lancez tous les services
docker-compose up -d

# Vérifiez les logs
docker-compose logs -f
```

Les services seront accessibles:
- Frontend: http://localhost:4200
- Chatbot API: http://localhost:9001
- GestionProduit API: http://localhost:9093
- MongoDB: mongodb://localhost:27017

### Développement Local

#### Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

#### Chatbot Service (Python)

```bash
cd chatbot-service
pip install -r requirements.txt
python -m uvicorn main:app --reload --port 9001
```

#### GestionProduit (Java)

```bash
cd GestionProduit
mvn spring-boot:run
```

## 📖 Documentation

La documentation technique détaillée se trouve dans le dossier `architecture/`:

- `README.md`: Vue d'ensemble de l'architecture
- `00-overview.md`: Introduction au système
- `03-logical-architecture.md`: Architecture logique
- `01-use-case-diagram.md`: Diagramme des cas d'utilisation
- `02-class-diagram.md`: Diagramme de classes
- `04-physical-architecture.md`: Architecture physique
- `05-sequence-diagram.md`: Diagrammes de séquence

## 🔧 Configuration

### Variables d'environnement

Créez un fichier `.env` à la racine du projet:

```env
# MongoDB
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=password
MONGO_INITDB_DATABASE=vermeg_db

# Google Gemini API
GOOGLE_GEMINI_API_KEY=votre_cle_api_ici

# Services
CHATBOT_API_URL=http://chatbot-service:9001/api
GESTIONPRODUIT_API_URL=http://gestionproduit:9093/api
```

## 📡 API Endpoints

### Chatbot Service
- `POST /api/chatbot/process`: Traitement de prompts avec l'IA
- `GET /api/chatbot/health`: Vérification de santé
- `GET /api/analytics`: Statistiques d'utilisation

### GestionProduit
- `GET /api/produits`: Liste des produits
- `POST /api/produits`: Créer un produit
- `PUT /api/produits/{id}`: Modifier un produit
- `DELETE /api/produits/{id}`: Supprimer un produit
- `GET /api/analytics`: Statistiques

## 🧪 Tests

```bash
# Tests Frontend
cd frontend
npm test

# Tests Chatbot Service
cd chatbot-service
pytest

# Tests GestionProduit
cd GestionProduit
mvn test
```

## 📝 Contribution

1. Fork le projet
2. Créez une branche (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📄 Licence

Ce projet est sous licence propriétaire Vermeg.

## 👥 Équipe

- Équipe Architecture Vermeg

## 📞 Support

Pour toute question ou problème, veuillez contacter l'équipe de support.

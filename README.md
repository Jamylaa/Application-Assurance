# Vermeg — Plateforme d'Assurance

Plateforme de gestion de produits d'assurance (produits, packs, garanties) avec un
assistant chatbot IA de configuration en langage naturel.

## Architecture

| Service | Rôle | Port hôte |
|---|---|---|
| `frontend` | Angular (servi par Nginx en prod, `ng serve` en dev) | 4200 |
| `gateway` | Spring Cloud Gateway — point d'entrée unique, vérifie les JWT Keycloak | 9091 |
| `eureka` | Spring Cloud Eureka — service discovery | 8761 |
| `gestionproduit` | Spring Boot — produits/packs/garanties, MongoDB `vermeg_db` | 9093 |
| `chatbot-service` | FastAPI (Python) — orchestrateur chatbot, MongoDB `vermeg_chatbot_db` | 9001 |
| `keycloak` | Authentification OIDC (realm `vermeg-realm`) | 9090 |
| `mongodb` | Base de données (2 bases logiques, 1 instance) | 27018 |

En développement, le frontend appelle `gestionproduit`/`chatbot-service` directement
(`environment.ts`, ports 9093/9001) en contournant le Gateway. En production
(`environment.prod.ts`), il passe par le Gateway (port 9091) qui route vers les deux
services et applique la vérification JWT.

## Prérequis

- Docker + Docker Compose
- Pour le développement en processus locaux (hors conteneurs) : JDK 21, Node 20, Python 3.11+

## Démarrage rapide (stack complète en conteneurs)

```bash
docker compose up -d
```

Démarre les 7 services. `gestionproduit`/`gateway`/`chatbot-service` attendent que
leurs dépendances (`mongodb`, `keycloak`, `eureka`) soient en bonne santé avant de
démarrer (`depends_on: condition: service_healthy`).

## Démarrage en développement (édition de code à chaud)

Pour éditer `GestionProduit`/`chatbot-service`/`frontend` sans reconstruire d'image
Docker à chaque changement :

```bash
docker compose up -d mongodb keycloak
```

puis, dans 3 terminaux séparés :

```bash
# GestionProduit (port 9093)
cd GestionProduit
MONGODB_URI="mongodb://admin:password@localhost:27018/vermeg_db?authSource=admin" ./mvnw spring-boot:run

# chatbot-service (port 9001)
cd chatbot-service
SPRING_BOOT_BASE_URL="http://localhost:9093/api" python -m uvicorn main:app --host 0.0.0.0 --port 9001

# frontend (port 4200)
cd frontend
npx ng serve --port 4200 --host 0.0.0.0 --configuration development
```

**Attention** : si Docker Desktop redémarre, il relance automatiquement tous les
conteneurs `restart: unless-stopped` qui tournaient avant — y compris
`gestionproduit`/`chatbot-service`/`frontend` en conteneurs, qui entreront alors en
conflit de port avec les process locaux ci-dessus. Vérifier `docker ps` après tout
redémarrage de Docker Desktop.

## Variables d'environnement

Copier `.env.example` → `.env` à la racine, et `chatbot-service/.env.example` →
`chatbot-service/.env`, puis renseigner au minimum :

| Variable | Utilisée par | Rôle |
|---|---|---|
| `JWT_SECRET` | Keycloak / services | Secret de signature JWT — **changer avant tout déploiement réel** |
| `MONGODB_URI`, `MONGODB_DATABASE` | GestionProduit | Connexion Mongo (base `vermeg_db`) |
| `MONGO_ROOT_USERNAME`, `MONGO_ROOT_PASSWORD` | MongoDB, GestionProduit, chatbot-service | Identifiants Mongo |
| `CHATBOT_MONGODB_URI`, `CHATBOT_MONGODB_DATABASE` | chatbot-service | Connexion Mongo dédiée (base `vermeg_chatbot_db`, historique/undo) |
| `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD` | Keycloak | Compte admin de bootstrap (dev uniquement) |
| `CORS_ALLOWED_ORIGINS` | GestionProduit, chatbot-service | Origines autorisées (obligatoire, sans valeur par défaut, en profils `staging`/`prod`) |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | GestionProduit | URL Eureka (obligatoire, sans valeur par défaut, en profils `staging`/`prod`) |
| `KEYCLOAK_ISSUER_URI` | GestionProduit | Issuer JWT attendu (obligatoire, sans valeur par défaut, en profils `staging`/`prod`) |
| `GITHUB_API_KEY` | chatbot-service | Clé pour l'extraction d'entités par LLM (GitHub AI Models) |

Ne jamais committer `.env` (déjà exclu par `.gitignore`).

## Profils Spring (GestionProduit)

`GestionProduit/src/main/resources/application.yml` définit 4 profils, activés via
`SPRING_PROFILES_ACTIVE` :

- `local` — développement en process local, MongoDB/Eureka sur `localhost`
- `docker` — développement via `docker compose up`, MongoDB/Eureka sur les noms de service du réseau Docker
- `staging` / `prod` — aucune valeur par défaut sur `MONGODB_URI`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`,
  `CORS_ALLOWED_ORIGINS`, `KEYCLOAK_ISSUER_URI` : ces 4 variables sont obligatoires
  (échec au démarrage si absentes, plutôt qu'un repli silencieux vers `localhost`).
  `prod` réduit aussi le niveau de log (`WARN`) et masque le détail de `/actuator/health`.

```bash
SPRING_PROFILES_ACTIVE=staging \
MONGODB_URI="mongodb://..." \
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://..." \
CORS_ALLOWED_ORIGINS="https://..." \
KEYCLOAK_ISSUER_URI="https://.../realms/vermeg-realm" \
java -jar GestionProduit.jar
```

## Build frontend par environnement

- `ng serve` (dev) utilise `environment.ts` (appels directs `localhost:9093`/`9001`).
- `ng build --configuration=production` (ou l'image Docker, qui l'utilise déjà)
  remplace automatiquement ce fichier par `environment.prod.ts` (`fileReplacements`
  dans `angular.json`), qui pointe vers le Gateway. Les valeurs par défaut de
  `environment.prod.ts` correspondent au Gateway exposé par `docker-compose.yml`
  (`localhost:9091`) ; pour un déploiement hors docker-compose, remplacer ces URLs
  par celles réellement joignables depuis le navigateur des utilisateurs.

## Sauvegarde / restauration MongoDB

```bash
# Sauvegarde les 2 bases (vermeg_db, vermeg_chatbot_db) dans backups/<horodatage>/
./scripts/backup-mongodb.sh

# Restaure depuis un dossier de sauvegarde (ecrase l'etat actuel des 2 bases)
./scripts/restore-mongodb.sh backups/20260722_143956
```

Nécessite que le conteneur `mongodb` tourne (`docker compose up -d mongodb`). Le
dossier `backups/` n'est jamais versionné (`.gitignore`).

## CI/CD

3 pipelines GitHub Actions indépendants (`.github/workflows/backend-ci.yml`,
`chatbot-ci.yml`, `frontend-ci.yml`), déclenchés sur push/PR et manuellement
(`workflow_dispatch`) une fois mergés sur la branche par défaut.

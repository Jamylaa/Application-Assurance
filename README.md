# Vermeg — Plateforme d'Assurance

<!-- Badges pointés sur la branche de travail actuelle ; remplacer ?branch=... par ?branch=main après merge. -->
[![Backend CI/CD](https://github.com/Jamylaa/Application-Assurance/actions/workflows/backend-ci.yml/badge.svg?branch=feature/python-chatbot-service)](https://github.com/Jamylaa/Application-Assurance/actions/workflows/backend-ci.yml)
[![Frontend CI/CD](https://github.com/Jamylaa/Application-Assurance/actions/workflows/frontend-ci.yml/badge.svg?branch=feature/python-chatbot-service)](https://github.com/Jamylaa/Application-Assurance/actions/workflows/frontend-ci.yml)
[![Chatbot CI/CD](https://github.com/Jamylaa/Application-Assurance/actions/workflows/chatbot-ci.yml/badge.svg?branch=feature/python-chatbot-service)](https://github.com/Jamylaa/Application-Assurance/actions/workflows/chatbot-ci.yml)
[![CodeQL](https://github.com/Jamylaa/Application-Assurance/actions/workflows/codeql.yml/badge.svg?branch=feature/python-chatbot-service)](https://github.com/Jamylaa/Application-Assurance/actions/workflows/codeql.yml)
[![Security (Trivy)](https://github.com/Jamylaa/Application-Assurance/actions/workflows/security.yml/badge.svg?branch=feature/python-chatbot-service)](https://github.com/Jamylaa/Application-Assurance/actions/workflows/security.yml)

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

5 pipelines GitHub Actions dans `.github/workflows/` :

| Pipeline | Fichier | Rôle |
|---|---|---|
| Backend CI/CD | `backend-ci.yml` | build + tests (matrice GestionProduit/Gateway/Eureka), image Docker, deploy k8s |
| Frontend CI/CD | `frontend-ci.yml` | `npm ci`, tests Karma headless, build prod, image Docker, deploy |
| Chatbot CI/CD | `chatbot-ci.yml` | `compileall` + pytest (si présent), image Docker, deploy |
| CodeQL (SAST) | `codeql.yml` | analyse statique de sécurité (Java, TypeScript, Python) |
| Security (Trivy) | `security.yml` | vulnérabilités des dépendances + mauvaises configs IaC |
| CD (kind éphémère) | `cd-kind.yml` | provisionne un cluster kind dans le runner, déploie Eureka (health-check) + valide tous les manifests |

Déclencheurs : push/PR sur `main`, `develop` et `feature/**`. Le projet vivant
actuellement sur une branche `feature/**`, les pipelines tournent **dès le push, sans
merge préalable sur `main`**.

**Déploiement (CD) désactivé par défaut.** Le push d'images Docker et le `kubectl` de
déploiement ne s'exécutent que si la variable de dépôt `DEPLOY_ENABLED=true` **et** sur
`main`. Sinon ces étapes sont *ignorées* (pipeline vert) plutôt qu'en échec faute de
secrets. Pour activer le CD, définir la variable `DEPLOY_ENABLED` et les secrets
`DOCKER_USERNAME`, `DOCKER_PASSWORD`, `KUBE_CONFIG`.

**CD auto-contenu (`cd-kind.yml`).** En complément — et sans aucun secret ni infra
externe — ce workflow provisionne un cluster **kind éphémère dans le runner**, y build +
déploie Eureka avec **vérification du rollout** (`/actuator/health`) et valide l'ensemble
des manifests (server dry-run), à chaque push. C'est la preuve de déployabilité de bout en
bout. Un déploiement full-stack (Mongo, GestionProduit, …) vise plutôt un cluster persistant.

## Sécurité (DevSecOps)

- **Trivy** (`security.yml`) : scan des vulnérabilités des dépendances (Maven/npm/pip) +
  secrets, et scan des mauvaises configurations IaC (Dockerfiles, manifests k8s). Résultats
  SARIF dans *Security > Code scanning*. Scan local : `make trivy`.
- **CodeQL** (`codeql.yml`) : analyse statique (SAST) Java / TypeScript / Python.
- **Dependabot** (`.github/dependabot.yml`) : PR hebdomadaires de mise à jour des
  dépendances Maven, npm, pip, images Docker et GitHub Actions.

## Kubernetes

Manifests dans `k8s/`. Chaque déploiement définit *resource requests/limits*,
*liveness/readiness probes* et un *startupProbe* (démarrage lent Spring Boot).

- Déploiement : `make k8s-deploy` (ou `kubectl apply -f k8s/`)
- Validation sans cluster : `make k8s-validate` (server dry-run)
- **Auto-scaling** (`k8s/hpa.yaml`) : HorizontalPodAutoscaler CPU/mémoire pour
  gestionproduit, gateway, frontend, chatbot-service (nécessite metrics-server). Eureka
  (registre) reste à réplicas fixes.
- **Ingress** (`k8s/ingress.yaml`) : `vermeg.local/api` → gateway, `vermeg.local/` →
  frontend (nécessite un Ingress Controller type ingress-nginx).

### Test local avec kind

```bash
make kind-up          # crée un cluster kind local
make build            # build les 5 images Docker
make kind-load        # charge les images dans le cluster
make k8s-deploy       # applique les manifests
make k8s-status       # pods / services / hpa
```

> Note : le ConfigMap `vermeg-config` fixe `SPRING_PROFILES_ACTIVE=k8s`, profil non défini
> dans les `application.yml` : les services s'appuient sur les variables injectées
> explicitement (ex. `SPRING_DATA_MONGODB_URI`). Fonctionnel, mais à formaliser (ajouter un
> vrai profil `k8s`) pour éviter tout repli implicite.

## Observabilité

- **Prometheus** scrape les métriques Micrometer (`/actuator/prometheus`) des services Java
  et `/metrics` du chatbot (`prometheus/prometheus.yml`).
- **Règles d'alerte** (`prometheus/alerts.yml`, validées par `promtool`) : InstanceDown,
  taux d'erreurs 5xx, latence p95, heap JVM, CPU — chargées via `rule_files` (aussi
  intégrées à la ConfigMap Prometheus k8s). Le routage des notifications nécessite un
  Alertmanager (non déployé).
- **Grafana** : dashboard par service `grafana/dashboards/microservices-overview.json`
  (disponibilité, débit, erreurs 5xx, latence p95, heap, CPU). Le provisioning
  (`grafana/provisioning/`) auto-charge datasource + dashboards en local ; en k8s il reste à
  monter dans le déploiement Grafana via ConfigMaps (sinon importer le JSON manuellement).

## Commandes (Makefile)

`make help` liste les cibles : `up`/`down`/`logs` (compose), `test`/`test-backend`/`test-frontend`,
`build`, `kind-up`/`kind-load`, `k8s-deploy`/`k8s-validate`/`k8s-status`, `trivy`.

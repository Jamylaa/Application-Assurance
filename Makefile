# Makefile — commandes courantes du projet Vermeg (dev, tests, Docker, Kubernetes, sécurité).
# Usage : make <cible>.  « make » ou « make help » liste les cibles disponibles.
# Prérequis selon la cible : docker, docker compose, kubectl, kind, JDK 21 (mvnw), Node 20 (npm).

SHELL := /bin/bash
KIND_CLUSTER ?= vermeg
K8S_NS ?= vermeg-assurance
JAVA_SERVICES := Eureka Gateway GestionProduit
IMAGES := eureka gateway gestionproduit chatbot-service frontend

.DEFAULT_GOAL := help

.PHONY: help
help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

# --- Docker Compose (stack complète) ---
.PHONY: up
up: ## Démarre toute la stack en conteneurs (docker compose up -d)
	docker compose up -d

.PHONY: down
down: ## Arrête la stack (docker compose down)
	docker compose down

.PHONY: logs
logs: ## Suit les logs de la stack
	docker compose logs -f --tail=100

# --- Tests ---
.PHONY: test-backend
test-backend: ## Lance les tests des 3 services Java
	for s in $(JAVA_SERVICES); do (cd $$s && ./mvnw -q test) || exit 1; done

.PHONY: test-frontend
test-frontend: ## Lance les tests Angular (headless)
	cd frontend && npm ci && npm test -- --watch=false --browsers=ChromeHeadless

.PHONY: test
test: test-backend test-frontend ## Lance tous les tests

# --- Images Docker ---
.PHONY: build
build: ## Build les 5 images Docker localement
	docker build -t vermeg-eureka:latest ./Eureka
	docker build -t vermeg-gateway:latest ./Gateway
	docker build -t vermeg-gestionproduit:latest ./GestionProduit
	docker build -t vermeg-chatbot-service:latest ./chatbot-service
	docker build -t vermeg-frontend:latest ./frontend

# --- Kubernetes (kind) ---
.PHONY: kind-up
kind-up: ## Crée un cluster kind local
	kind create cluster --name $(KIND_CLUSTER)

.PHONY: kind-down
kind-down: ## Supprime le cluster kind
	kind delete cluster --name $(KIND_CLUSTER)

.PHONY: kind-load
kind-load: ## Charge les 5 images dans le cluster kind
	for i in $(IMAGES); do kind load docker-image vermeg-$$i:latest --name $(KIND_CLUSTER); done

.PHONY: k8s-validate
k8s-validate: ## Valide tous les manifests contre l'API (server dry-run)
	kubectl apply -f k8s/ --dry-run=server

.PHONY: k8s-deploy
k8s-deploy: ## Applique tous les manifests k8s
	kubectl apply -f k8s/

.PHONY: k8s-status
k8s-status: ## Affiche pods / services / hpa du namespace
	kubectl -n $(K8S_NS) get pods,svc,hpa

.PHONY: k8s-delete
k8s-delete: ## Supprime les ressources k8s
	kubectl delete -f k8s/ --ignore-not-found

# --- Sécurité ---
.PHONY: trivy
trivy: ## Scan de vulnérabilités local (Trivy via Docker)
	docker run --rm -v "$$(pwd):/src:ro" aquasec/trivy:latest fs --scanners vuln --severity CRITICAL,HIGH --ignore-unfixed /src

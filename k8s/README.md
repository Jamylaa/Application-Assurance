# Kubernetes Deployment for Vermeg Assurance Platform

## Prerequisites
- kubectl installed
- Kubernetes cluster (Minikube, Kind, or cloud provider)
- Docker images pushed to registry

## Deployment Steps

### 1. Create Namespace
```bash
kubectl apply -f namespace.yaml
```

### 2. Create ConfigMap and Secret
```bash
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
```

### 3. Deploy Infrastructure Services
```bash
kubectl apply -f mongodb-statefulset.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f keycloak-deployment.yaml
```

### 4. Deploy Eureka Server
```bash
kubectl apply -f eureka-deployment.yaml
```

### 5. Deploy Microservices
```bash
kubectl apply -f gestionproduit-deployment.yaml
kubectl apply -f gateway-deployment.yaml
kubectl apply -f frontend-deployment.yaml
```

### 6. Deploy Monitoring Stack
```bash
kubectl apply -f prometheus-deployment.yaml
kubectl apply -f grafana-deployment.yaml
```

## Verify Deployment
```bash
kubectl get pods -n vermeg-assurance
kubectl get services -n vermeg-assurance
```

## Access Services
- Gateway: LoadBalancer external IP:9091
- Frontend: LoadBalancer external IP
- Grafana: Port-forward to 3000
- Prometheus: Port-forward to 9090

## Cleanup
```bash
kubectl delete namespace vermeg-assurance
```

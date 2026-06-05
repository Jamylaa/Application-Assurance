# Vermeg Assurance Platform - PFE Documentation

## Overview
This is a microservices-based insurance management platform built with Spring Boot, Angular 18, and MongoDB. The platform provides intelligent product recommendations using Google Gemini AI.

## Architecture

### Microservices Architecture
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Gateway   │────▶│   Eureka    │
│  (Angular)  │     │ (Spring)    │     │ (Discovery) │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │GestionProduit│ │ GestionUser │ │  Keycloak   │
    │  (Spring)    │ │  (Spring)   │ │  (Auth)     │
    └─────────────┘ └─────────────┘ └─────────────┘
           │               │
           └───────┬───────┘
                   ▼
            ┌─────────────┐
            │  MongoDB    │
            └─────────────┘
```

### Services

1. **Eureka Server** (Port 8761)
   - Service discovery and registration
   - Load balancing support

2. **API Gateway** (Port 8080)
   - Single entry point for all requests
   - Routing to microservices
   - Security validation

3. **GestionUser** (Port 9092)
   - User management
   - Authentication with Keycloak
   - Role-based access control

4. **GestionProduit** (Port 9093)
   - Product management
   - Pack and Guarantee configuration
   - AI-powered recommendations
   - Business scoring engine

5. **Frontend** (Port 4200)
   - Angular 18 SPA
   - Keycloak authentication
   - Responsive UI with Primeng

6. **Keycloak** (Port 9090)
   - Identity and Access Management
   - JWT token generation
   - User authentication

7. **MongoDB** (Port 27017)
   - Document database
   - Data persistence

## Technology Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.2.3** - Application framework
- **Spring Cloud** - Microservices patterns
- **Spring Security OAuth2** - Security
- **Spring Data MongoDB** - Database access
- **Eureka** - Service discovery
- **Keycloak 26** - Identity management
- **Google Gemini AI** - AI recommendations
- **Micrometer Prometheus** - Monitoring

### Frontend
- **Angular 18** - Frontend framework
- **TypeScript** - Type-safe JavaScript
- **Primeng** - UI component library
- **Keycloak Angular** - Authentication
- **RxJS** - Reactive programming

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local orchestration
- **Kubernetes** - Container orchestration
- **GitHub Actions** - CI/CD
- **Prometheus** - Monitoring
- **Grafana** - Visualization

## Data Model

### Core Entities

**Produit** (Product)
- Insurance product definition
- Type: Auto, Santé, Habitation
- Associated with multiple Packs

**Pack** (Package)
- Group of guarantees
- Pricing and coverage details
- Reference to Produit

**Garantie** (Guarantee)
- Insurance guarantee coverage
- Financial parameters
- Categorized by DomaineSante

**PackGarantie** (Intermediate Entity)
- N:N relationship between Pack and Garantie
- TauxRemboursement, Plafond, Franchise
- Condition and display order

### Domain Model
```
DomaineSante (Medical Domains)
    ├── SOINS_COURANTS
    ├── HOSPITALISATION
    ├── OPTIQUE
    ├── DENTAIRE
    ├── PHARMACIE
    └── ASSISTANCE_INTERNATIONAL

TypeGarantie (Guarantee Types)
    └── Mapped to DomaineSante
```

## Key Features

### 1. AI-Powered Recommendations
- Integration with Google Gemini AI
- Client profile analysis
- Weighted scoring engine
- Personalized product suggestions

### 2. Business Scoring Engine
- Configurable criteria weights
- Age-based scoring
- Budget matching
- Coverage analysis

### 3. Dynamic Breadcrumb
- Automatic route-based breadcrumbs
- Support for nested routes (add/edit/details)
- Icon-enhanced navigation

### 4. Toast Notifications
- Success, error, warning, info types
- Auto-dismissal
- Animated transitions

### 5. Dark Mode
- System preference detection
- Manual toggle
- Persistent storage
- CSS variable-based theming

### 6. Swagger/OpenAPI Documentation
- Auto-generated API docs
- JWT security scheme
- Available at /swagger-ui.html

### 7. Monitoring
- Prometheus metrics
- Grafana dashboards
- Health checks
- Actuator endpoints

## API Documentation

### GestionProduit API
- Base URL: http://localhost:9093
- Swagger: http://localhost:9093/swagger-ui.html

**Key Endpoints:**
- GET /api/produits - List all products
- POST /api/produits - Create product
- PUT /api/produits/{id} - Update product
- DELETE /api/produits/{id} - Delete product
- GET /api/packs - List all packs
- GET /api/garanties/domain - Get guarantees by domain
- POST /api/recommendations - Get AI recommendations

### GestionUser API
- Base URL: http://localhost:9092
- Swagger: http://localhost:9092/swagger-ui.html

**Key Endpoints:**
- GET /api/users - List all users
- POST /api/users - Create user
- POST /api/auth/logout - Logout
- GET /api/auth/me - Get current user

## Deployment

### Local Development
```bash
# Start all services
docker-compose up -d

# Access services
- Frontend: http://localhost:4200
- Gateway: http://localhost:8080
- Eureka: http://localhost:8761
- GestionProduit: http://localhost:9093
- GestionUser: http://localhost:9092
- Keycloak: http://localhost:9090
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/
kubectl get pods -n vermeg-assurance
kubectl get svc -n vermeg-assurance
```

### Monitoring
```bash
cd monitoring
docker-compose -f docker-compose.monitoring.yml up -d
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

## Configuration

### Environment Variables
See `frontend/src/environments/environment.ts` for frontend configuration.

Backend configuration in `application.yml` files for each service.

### Keycloak Configuration
- Realm: vermeg-realm
- Client: frontend-client
- URL: http://localhost:9090

## Security

### Authentication Flow
1. User accesses frontend
2. Redirected to Keycloak login
3. Keycloak returns JWT token
4. Frontend includes token in API requests
5. Backend validates JWT via Keycloak JWKS endpoint
6. Access granted based on roles

### CORS Configuration
Configured in SecurityConfig to allow frontend access.

## Testing

### Backend Tests
```bash
cd GestionProduit
mvn test

cd GestionUser
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## CI/CD

GitHub Actions workflow:
- Triggers on push to main/develop
- Builds all services
- Runs tests
- Builds Docker images
- Pushes to Docker Hub

## Performance Optimization

- MongoDB indexing on frequently queried fields
- Caching with Caffeine
- Lazy loading for large datasets
- CDN for static assets
- Minification and tree-shaking in Angular

## Future Enhancements

- RAG (Retrieval-Augmented Generation) for AI
- Conversational memory for chatbot
- Advanced analytics dashboard
- Mobile app development
- Multi-tenancy support

## Team

- **Backend**: Spring Boot microservices
- **Frontend**: Angular 18 SPA
- **DevOps**: Docker, Kubernetes, CI/CD
- **AI**: Google Gemini integration

## License

Private - Vermeg

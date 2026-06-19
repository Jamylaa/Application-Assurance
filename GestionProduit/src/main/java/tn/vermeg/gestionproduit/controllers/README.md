# Controllers Package

## 📋 Description

Ce package contient tous les contrôleurs REST du microservice GestionProduit. Les contrôleurs exposent les API REST et gèrent les requêtes HTTP.

## 🏗️ Structure

```
controllers/
├── GarantieController.java       # Contrôleur des garanties
├── PackUnifiedController.java   # Contrôleur des packs
├── ProduitController.java       # Contrôleur des produits
├── ChatbotController.java       # Contrôleur du chatbot
├── MemoryController.java        # Contrôleur de la mémoire
├── RAGController.java           # Contrôleur RAG
└── AIAnalyticsController.java   # Contrôleur des analytics
```

## 📝 Contrôleurs

### GarantieController

**Base Path :** `/api/garanties`

**Endpoints :**
- `GET /` - Liste toutes les garanties
- `GET /{idGarantie}` - Récupère une garantie par ID
- `GET /type/{type}` - Filtre les garanties par type
- `GET /statut/{statut}` - Filtre les garanties par statut
- `GET /search` - Recherche des garanties par nom
- `GET /taux-min/{tauxMin}` - Filtre par taux de remboursement minimum
- `GET /plafond-min/{plafondMin}` - Filtre par plafond minimum
- `GET /domaine/{domaine}` - Filtre par domaine médical
- `GET /domaine/{domaine}/statut/{statut}` - Filtre par domaine et statut
- `GET /domaine/{domaine}/actives` - Filtre les garanties actives par domaine
- `GET /domaines` - Liste tous les domaines médicaux
- `POST /` - Crée une nouvelle garantie
- `PUT /{idGarantie}` - Met à jour une garantie
- `PATCH /{idGarantie}/desactiver` - Désactive une garantie
- `DELETE /{idGarantie}` - Supprime une garantie

### PackUnifiedController

**Base Path :** `/api/packs`

**Endpoints :**
- `GET /` - Liste tous les packs
- `GET /search` - Recherche des packs par nom
- `GET /statut/{statut}` - Filtre les packs par statut
- `GET /niveau/{niveauCouverture}` - Filtre par niveau de couverture
- `GET /type-client/{typeClient}` - Filtre par type de client
- `GET /prix-range` - Filtre par plage de prix
- `GET /by-produit/{produitId}` - Récupère les packs par produit
- `GET /produit/{produitId}` - Alias pour /by-produit
- `GET /associations` - Liste toutes les associations pack-garantie
- `GET /statistics` - Statistiques des packs
- `GET /health` - Health check du service
- `POST /` - Crée un nouveau pack
- `GET /{packId}/garanties` - Récupère les garanties d'un pack
- `GET /{packId}/garanties/optionnelles` - Récupère les garanties optionnelles
- `GET /{packId}/garanties/obligatoires` - Récupère les garanties obligatoires
- `GET /{packId}/garanties/incluses` - Alias pour garanties obligatoires
- `GET /{packId}/garanties-disponibles` - Récupère les garanties disponibles
- `GET /{packId}/prix-total` - Calcule le prix total d'un pack
- `POST /{packId}/garanties/{garantieId}` - Ajoute une garantie à un pack
- `POST /{packId}/associate-produit/{produitId}` - Associe un pack à un produit
- `DELETE /{packId}/dissociate-produit` - Dissocie un pack d'un produit
- `PUT /{idPack}` - Met à jour un pack
- `DELETE /{idPack}` - Supprime un pack
- `PATCH /{idPack}/desactiver` - Désactive un pack
- `PUT /associations/{id}` - Met à jour une association pack-garantie
- `DELETE /associations/{id}` - Supprime une association pack-garantie
- `PATCH /associations/{id}/activation` - Active/désactive une garantie dans un pack
- `GET /{id}` - Récupère un pack par ID

### ProduitController

**Base Path :** `/api/produits`

**Endpoints :**
- `GET /` - Liste tous les produits
- `GET /{idProduit}` - Récupère un produit par ID
- `GET /type/{typeProduit}` - Filtre les produits par type
- `GET /statut/{statut}` - Filtre les produits par statut
- `GET /search` - Recherche des produits par nom
- `POST /` - Crée un nouveau produit
- `PUT /{idProduit}` - Met à jour un produit
- `PATCH /{idProduit}/desactiver` - Désactive un produit
- `DELETE /{idProduit}` - Supprime un produit

### ChatbotController

**Base Path :** `/api/chatbot`

**Endpoints :**
- `POST /` - Traite un prompt utilisateur

### MemoryController

**Base Path :** `/api/memory`

**Endpoints :**
- Gestion de la mémoire conversationnelle

### RAGController

**Base Path :** `/api/rag`

**Endpoints :**
- `POST /query` - Effectue une requête RAG
- `POST /index` - Indexe des documents

### AIAnalyticsController

**Base Path :** `/api/analytics`

**Endpoints :**
- Analytics et métriques IA

## 🔧 Bonnes Pratiques

### Création d'un Nouveau Contrôleur

1. **Annoter avec @RestController**
```java
@RestController
@RequestMapping("/api/ressource")
@CrossOrigin(origins = "http://localhost:4200")
public class MonController {
    // implémentation
}
```

2. **Injection des services via constructeur**
```java
private final MonService service;

public MonController(MonService service) {
    this.service = service;
}
```

3. **Documentation Swagger**
```java
@Operation(summary = "Description de l'endpoint")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Succès"),
    @ApiResponse(responseCode = "404", description = "Non trouvé")
})
```

4. **Validation des entrées**
```java
@PostMapping
public ResponseEntity<Entity> create(@Valid @RequestBody Entity entity) {
    // validation automatique
}
```

5. **Gestion des réponses**
```java
return ResponseEntity.ok(result);              // 200 OK
return ResponseEntity.status(HttpStatus.CREATED).body(result);  // 201 Created
return ResponseEntity.notFound().build();      // 404 Not Found
return ResponseEntity.badRequest().build();    // 400 Bad Request
```

### Conventions de Nommage

- **Classe :** `XxxController`
- **Base Path :** `/api/xxx` (pluriel, minuscule)
- **Méthodes :** Verbes HTTP + nom de la ressource
  - `GET /api/produits` - Liste
  - `GET /api/produits/{id}` - Détail
  - `POST /api/produits` - Création
  - `PUT /api/produits/{id}` - Mise à jour
  - `DELETE /api/produits/{id}` - Suppression

### Codes de Réponse

- **200 OK** - Succès de la requête
- **201 Created** - Ressource créée avec succès
- **400 Bad Request** - Données invalides
- **404 Not Found** - Ressource non trouvée
- **500 Internal Server Error** - Erreur serveur

### CORS Configuration

Tous les contrôleurs sont configurés avec :
```java
@CrossOrigin(origins = "http://localhost:4200")
```

## 📊 Dépendances

Les contrôleurs dépendent généralement de :
- **Services** : Pour la logique métier
- **Entities/DTOs** : Pour les données de requête/réponse
- **Jakarta Validation** : Pour la validation des entrées

## 🔍 Tests

Chaque contrôleur doit avoir des tests d'intégration couvrant :
- Les endpoints CRUD
- Les cas d'erreur
- La validation des entrées
- Les codes de réponse

---

**Dernière mise à jour :** 2024

# Guide de Conventions de Nommage

## 📋 Introduction

Ce document définit les conventions de nommage à suivre pour assurer la cohérence et la lisibilité du code dans le projet GestionProduit.

## 🏗️ Conventions Générales

### Langage
- **Langage principal :** Java
- **Style :** Conventions de code Java standard

### Encodage
- **Encodage des fichiers :** UTF-8

## 📁 Conventions de Packages

### Structure des Packages
```
tn.vermeg.gestionproduit/
├── config/           # Configurations (singulier)
├── controllers/      # Contrôleurs REST (pluriel)
├── dto/             # Data Transfer Objects (singulier)
├── entities/        # Entités métier (pluriel)
├── enums/           # Énumérations (pluriel)
├── exceptions/      # Exceptions (pluriel)
├── repositories/    # Repositories (pluriel)
└── services/        # Services (pluriel)
    ├── scoring/     # Services de scoring
    ├── kafka/
    └── metrics/
```

### Règles de Nommage des Packages
- **Tout en minuscule**
- **Séparateur :** point (.)
- **Structure hiérarchique :** du général au spécifique
- **Sous-packages :** en minuscule, singulier ou pluriel selon le contexte

## 📝 Conventions de Classes

### Classes Métier (Entities)
```java
// Format : Nom au singulier, PascalCase
public class Produit { }
public class Pack { }
public class Garantie { }
public class PackGarantie { }
```

### DTOs (Data Transfer Objects)
```java
// Format : Nom + DTO, PascalCase
public class ProduitDTO { }
public class RecommendationResponseDTO { }
```

### Services
```java
// Format : Nom de l'entité + Service, PascalCase
public class ProduitService { }
public class GarantieService { }
public class PackUnifiedService { }
```

### Contrôleurs
```java
// Format : Nom de l'entité + Controller, PascalCase
public class ProduitController { }
public class GarantieController { }
public class PackUnifiedController { }
```

### Repositories
```java
// Format : Nom de l'entité + Repository, PascalCase
public interface ProduitRepository { }
public interface GarantieRepository { }
public interface PackUnifiedRepository { }
```

### Exceptions
```java
// Format : Nom + Exception, PascalCase
public class ResourceNotFoundException extends RuntimeException { }
public class ValidationException extends RuntimeException { }
```

### Enums
```java
// Format : Nom au singulier ou pluriel selon le contexte, PascalCase
public enum Statut { }
public enum TypeClient { }
public enum TypeProduit { }
```

### Configurations
```java
// Format : Nom + Config, PascalCase
@Configuration
public class CorsConfig { }
public class KafkaConfig { }
public class RedisConfig { }
```

## 🔤 Conventions de Méthodes

### Méthodes CRUD
```java
// CREATE
public Produit createProduit(Produit produit) { }
public Pack createPack(Pack pack) { }

// READ
public Produit getProduitById(String id) { }
public List<Produit> getAllProduits() { }
public List<Produit> findByXxx(String xxx) { }

// UPDATE
public Produit updateProduit(String id, Produit details) { }

// DELETE
public void deleteProduit(String id) { }
```

### Méthodes de Recherche
```java
// Format : find + By + Critère
public List<Produit> findByNomContaining(String nom) { }
public List<Pack> findByStatut(Statut statut) { }
public List<Garantie> findByDomaine(DomaineMedical domaine) { }
```

### Méthodes de Validation
```java
// Format : validate + Nom
public void validateProduit(Produit produit) { }
public boolean isValidGarantie(Garantie garantie) { }
```

### Méthodes de Conversion/Mapping
```java
// Format : to + Destination
public ProduitDTO toDTO(Produit produit) { }
public Produit toEntity(ProduitDTO dto) { }
```

### Méthodes Booléennes
```java
// Format : is/has/can + Verbe
public boolean isActive() { }
public boolean hasGaranties() { }
public boolean canBeDeleted() { }
```

## 📊 Conventions de Variables

### Variables Locales
```java
// Format : camelCase
String nomProduit = "";
double prixMensuel = 0.0;
List<Pack> packs = new ArrayList<>();
```

### Variables de Classe (Champs)
```java
// Format : camelCase
private String nomProduit;
private double prixMensuel;
private List<Pack> packs;
```

### Variables Constantes
```java
// Format : UPPER_SNAKE_CASE
private static final String DEFAULT_VALUE = "DEFAULT";
private static final int MAX_RETRIES = 3;
```

### Variables de Collections
```java
// Format : Nom au pluriel, camelCase
List<Produit> produits = new ArrayList<>();
Map<String, Garantie> garanties = new HashMap<>();
Set<TypeClient> typeClients = new HashSet<>();
```

## 🎯 Conventions de Paramètres

```java
// Format : camelCase, descriptif
public void createProduit(String nomProduit, double prix) { }
public void updatePack(String packId, Pack packDetails) { }
```

## 🔗 Conventions d'API REST

### Endpoints
```java
// Format : /api/ressource (pluriel, minuscule)
@RequestMapping("/api/produits")
@RequestMapping("/api/packs")
@RequestMapping("/api/garanties")
```

### Variables de Path
```java
// Format : nom au singulier, camelCase
@GetMapping("/{produitId}")
@GetMapping("/{packId}/garanties/{garantieId}")
```

### Paramètres de Query
```java
// Format : camelCase
@GetMapping("/search?nomPack=xxx")
@GetMapping("/prix-range?min=100&max=500")
```

## 🏷️ Conventions d'Annotations

### Annotations Spring
```java
@Service                    // Classes de service
@Repository                 // Interfaces de repository
@RestController             // Classes de contrôleur
@RequestMapping             // Mapping de base
@GetMapping                 // Mapping GET
@PostMapping                 // Mapping POST
@PutMapping                 // Mapping PUT
@DeleteMapping              // Mapping DELETE
@PatchMapping               // Mapping PATCH
@Autowired                  // Injection (préférer constructeur)
@Transactional              // Transactions
```

### Annotations de Validation
```java
@Valid                      // Validation automatique
@NotNull                    // Non null
@NotBlank                  // Non null et non vide
@Size                       // Taille
@Min/@Max                  // Valeurs min/max
@Email                     // Format email
```

### Annotations Javadoc
```java
/**
 * Description de la classe/méthode.
 *
 * @author Nom de l'auteur
 * @since Version
 * @param description des paramètres
 * @return description du retour
 * @throws description des exceptions
 */
```

## 📚 Conventions de Documentation

### Javadoc
- **Classes :** Toujours documenter les classes publiques
- **Méthodes publiques :** Toujours documenter
- **Méthodes privées :** Documenter si la logique est complexe
- **Paramètres :** @param pour chaque paramètre
- **Retour :** @return avec description
- **Exceptions :** @throws avec description

### Commentaires de Code
```java
// Commentaire sur une ligne
/*
 * Commentaire
 * sur plusieurs
 * lignes
 */
```

## 🎨 Conventions de Formatage

### Indentation
- **Taille :** 4 espaces (pas de tabulations)

### Longueur des Lignes
- **Maximum :** 120 caractères

### Espacement
- **Opérateurs :** Espaces autour des opérateurs
- **Virgules :** Espace après les virgules
- ** accolades :** Ouverture sur la même ligne

### Ordre des Imports
1. Imports Java standard
2. Imports Jakarta/Spring
3. Imports du projet (tn.vermeg.gestionproduit)
4. Imports statiques

## 🚫 Interdictions

- ❌ Noms de variables en une seule lettre (sauf boucles)
- ❌ Abréviations non standard
- ❌ Noms de classes en minuscule
- ❌ Caractères spéciaux dans les noms
- ❌ Mots réservés du langage comme noms de variables
- ❌ Code commenté (supprimer au lieu de commenter)

## ✅ Checklist de Review

Avant de committer :
- [ ] Conventions de nommage respectées
- [ ] Javadoc complète sur les classes publiques
- [ ] Javadoc sur les méthodes publiques
- [ ] Code formaté selon les standards
- [ ] Aucun code mort ou commenté
- [ ] Variables nommées de manière descriptive
- [ ] Méthodes courtes et focales (< 50 lignes)
- [ ] Pas de duplication de code

---

**Version :** 1.0  
**Dernière mise à jour :** 2024

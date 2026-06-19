# Guide de Migration - Design System

## Résumé de l'Avancement

### ✅ Complété

#### 1. Dashboard Component
**Fichier:** `src/app/pages/dashboard/dashboard.component.html`
- ✅ Ajouté animations d'entrée (animate-fade-in-down, animate-fade-in-up)
- ✅ Mis à jour les cartes KPI avec nouvelles classes (stat-card--primary, stat-card--info, etc.)
- ✅ Mis à jour les métriques avancées avec nouvelles classes (metric-card--primary, etc.)
- ✅ Ajouté effets hover (hover-lift)
- ✅ Mis à jour les graphiques avec nouvelles classes (chart-card--primary, etc.)
- ✅ Mis à jour les actions rapides avec nouvelles classes (quick-action-btn--primary, etc.)

#### 2. Garanties Component (Liste)
**Fichiers:** 
- `src/app/pages/garanties/garanties.component.html`
- `src/app/pages/garanties/garanties.component.ts`

**Modifications HTML:**
- ✅ Ajouté animation au header (animate-fade-in-down)
- ✅ Ajouté animation à la table card (animate-fade-in-up)
- ✅ Ajouté wrapper responsive-table
- ✅ Changé styleClass de "custom-table" à "p-table"
- ✅ Remplacé les badges par le composant UiBadge
- ✅ Mis à jour les actions avec table-actions class
- ✅ Mis à jour l'état vide avec table-empty class

**Modifications TypeScript:**
- ✅ Importé UiBadgeComponent
- ✅ Ajouté UiBadgeComponent aux imports
- ✅ Ajouté méthode getStatusBadgeVariant()

#### 3. Packs Component (Liste)
**Fichiers:**
- `src/app/pages/packs/packs.component.html`
- `src/app/pages/packs/packs.component.ts`

**Modifications HTML:**
- ✅ Ajouté animation au header (animate-fade-in-down)
- ✅ Ajouté animation à la table card (animate-fade-in-up)
- ✅ Ajouté wrapper responsive-table
- ✅ Changé styleClass de "custom-table" à "p-table"
- ✅ Remplacé les badges par le composant UiBadge
- ✅ Mis à jour les actions avec table-actions class
- ✅ Mis à jour l'état vide avec table-empty class
- ✅ Fermé correctement la div responsive-table

**Modifications TypeScript:**
- ✅ Importé UiBadgeComponent
- ✅ Ajouté UiBadgeComponent aux imports
- ✅ Ajouté méthode getStatusBadgeVariant()
- ✅ Ajouté méthode getNiveauBadgeVariant()

### ⏳ À Faire Manuellement

#### 1. Produits Component (Liste)
**Fichier:** `src/app/pages/produits/produits.component.html`

**Modifications requises:**
```html
<!-- Page Header -->
<div class="page-header produits-header animate-fade-in-down">

<!-- Table Card -->
<div class="table-card animate-fade-in-up">

<!-- Ajouter wrapper responsive-table -->
<div class="responsive-table">
  <p-table styleClass="p-table">

<!-- Fermer le wrapper -->
</div> <!-- responsive-table -->

<!-- Remplacer les badges -->
<app-ui-badge variant="info" size="sm">{{ produit.typeProduit || 'N/A' }}</app-ui-badge>
<app-ui-badge [variant]="getStatusBadgeVariant(produit.statut)" size="sm">

<!-- Remplacer les actions -->
<div class="table-actions">
  <button class="action-btn view-btn">
  <button class="action-btn edit-btn">
  <button class="action-btn delete-btn">

<!-- Mettre à jour l'état vide -->
<div class="table-empty">
  <h3 class="empty-title">
  <p class="empty-description">
```

**Modifications TypeScript requises:**
```typescript
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';

// Ajouter aux imports
imports: [
  // ... autres imports
  UiBadgeComponent
]

// Ajouter méthode
getStatusBadgeVariant(statut: string): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (statut?.toUpperCase()) {
    case 'ACTIF': return 'success';
    case 'INACTIF': return 'error';
    default: return 'neutral';
  }
}
```

#### 2. Formulaires
**Fichiers:**
- `src/app/pages/garanties/garantie-form.component.html`
- `src/app/pages/packs/pack-form.component.html`
- `src/app/pages/produits/produit-form.component.html`

**Modifications générales requises:**
```html
<!-- Changer la classe du formulaire -->
<form class="form-container">

<!-- Ajouter animation au header -->
<div class="page-header [nom]-header animate-fade-in-down">

<!-- Ajouter animation à la form card -->
<div class="form-card animate-fade-in-up">

<!-- Remplacer les classes de champ -->
<div class="form-group">
  <label class="form-label form-label--required">
  <div class="form-control-wrap" [class.form-control-wrap--invalid]="...">
    <input class="form-control" />
  </div>
  <div class="form-feedback form-feedback--invalid">
```

#### 3. Autres Composants
**Fichiers à mettre à jour:**
- `src/app/pages/garanties/garantie-details.component.html`
- `src/app/pages/packs/pack-details.component.html`
- `src/app/pages/produits/produit-details.component.html`
- `src/app/pages/users/users.component.html`
- `src/app/pages/unified-chatbot/unified-chatbot.component.html`

## Instructions de Migration

### Étape 1: Importer les composants UI nécessaires
Dans chaque composant TypeScript, ajoutez:
```typescript
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
```

### Étape 2: Ajouter aux imports
```typescript
imports: [
  // ... vos imports existants
  UiBadgeComponent,
  UiButtonComponent,
  UiCardComponent
]
```

### Étape 3: Appliquer les classes CSS
Remplacez les anciennes classes par les nouvelles:
- `auth-form` → `form-container`
- `form-field` → `form-group`
- `field-label` → `form-label`
- `input-wrap` → `form-control-wrap`
- `auth-input` → `form-control`
- `field-error` → `form-feedback form-feedback--invalid`
- `custom-table` → `p-table`
- `empty-state` → `table-empty`

### Étape 4: Ajouter les animations
Aux conteneurs principaux, ajoutez:
- `animate-fade-in-down` (pour les headers)
- `animate-fade-in-up` (pour les cartes et tables)
- `hover-lift` (pour les éléments interactifs)

### Étape 5: Utiliser les composants UI
Remplacez les badges et boutons par les nouveaux composants:
```html
<!-- Badge -->
<app-ui-badge variant="success" size="sm">Texte</app-ui-badge>

<!-- Button -->
<button app-ui-button variant="primary" size="md">Texte</button>
```

## Classes CSS Principales

### Animations
- `.animate-fade-in-up` - Fade in avec mouvement vers le haut
- `.animate-fade-in-down` - Fade in avec mouvement vers le bas
- `.hover-lift` - Élévation au hover

### Layout
- `.form-container` - Conteneur de formulaire
- `.table-card` - Carte de tableau
- `.page-header` - Header de page

### Formulaires
- `.form-group` - Groupe de formulaire
- `.form-label` - Label de formulaire
- `.form-label--required` - Label requis
- `.form-control` - Contrôle de formulaire
- `.form-control-wrap` - Wrapper de contrôle
- `.form-control-wrap--invalid` - Wrapper invalide
- `.form-feedback` - Feedback de validation
- `.form-feedback--invalid` - Feedback invalide

### Tableaux
- `.responsive-table` - Wrapper responsive
- `.p-table` - Classe PrimeNG
- `.table-actions` - Actions de tableau
- `.table-empty` - État vide
- `.empty-title` - Titre vide
- `.empty-description` - Description vide

## Méthodes TypeScript à Ajouter

### getStatusBadgeVariant
```typescript
getStatusBadgeVariant(statut: string): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (statut?.toUpperCase()) {
    case 'ACTIF': return 'success';
    case 'INACTIF': return 'error';
    default: return 'neutral';
  }
}
```

### getNiveauBadgeVariant (pour packs)
```typescript
getNiveauBadgeVariant(niveau: string): 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (niveau?.toLowerCase()) {
    case 'premium': case 'gold': case 'platinum': return 'success';
    case 'standard': case 'silver': return 'info';
    case 'basic': case 'bronze': return 'warning';
    default: return 'neutral';
  }
}
```

## Test et Vérification

Après avoir appliqué les modifications:

1. **Redémarrer le serveur de développement:**
   ```bash
   ng serve
   ```

2. **Vérifier les composants mis à jour:**
   - Dashboard - animations et nouvelles classes
   - Garanties - badges UiBadge et tableaux stylisés
   - Packs - badges UiBadge et tableaux stylisés

3. **Tester la responsivité:**
   - Redimensionner la fenêtre du navigateur
   - Tester sur différentes tailles d'écran

4. **Tester le dark mode:**
   - Ajouter la classe `my-app-dark` au body
   - Vérifier que les styles s'appliquent correctement

## Problèmes Connus et Solutions

### Problème: Les modifications ne s'appliquent pas
**Solution:** Vider le cache du navigateur et redémarrer le serveur de développement

### Problème: Les classes ne sont pas reconnues
**Solution:** Vérifier que `styles.scss` est bien importé dans `angular.json`

### Problème: Les composants UI ne fonctionnent pas
**Solution:** Vérifier que les composants sont bien importés dans le module ou le composant

## Support

Pour toute question ou problème, consultez la documentation complète dans `DESIGN-SYSTEM.md` ou le guide d'intégration dans `INTEGRATION-GUIDE.md`.

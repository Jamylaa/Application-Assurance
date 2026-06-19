# Design System - Vermeg Assurance
## Documentation Complète du Système de Design Frontend

**Version:** 2.0.0  
**Niveau:** PFE Ingénieur  
**Dernière mise à jour:** 2024

---

## Table des Matières

1. [Introduction](#introduction)
2. [Architecture du Design System](#architecture)
3. [Palette de Couleurs](#palette-de-couleurs)
4. [Typographie](#typographie)
5. [Espacement](#espacement)
6. [Bordures et Coins Arrondis](#bordures-et-coins-arrondis)
7. [Ombres](#ombres)
8. [Transitions et Animations](#transitions-et-animations)
9. [Composants UI](#composants-ui)
10. [Responsive Design](#responsive-design)
11. [Dark Mode](#dark-mode)
12. [Bonnes Pratiques](#bonnes-pratiques)

---

## Introduction

Ce design system a été créé pour harmoniser l'interface utilisateur de l'application Vermeg Assurance avec un niveau de qualité professionnelle digne d'un Projet de Fin d'Études (PFE) ingénieur. Il fournit une base cohérente pour tous les composants UI et garantit une expérience utilisateur fluide et moderne.

### Objectifs

- **Cohérence visuelle** : Design uniforme à travers toute l'application
- **Accessibilité** : Respect des standards WCAG AA
- **Performance** : Optimisation des animations et transitions
- **Maintenabilité** : Code modulaire et réutilisable
- **Scalabilité** : Facile à étendre et à personnaliser

---

## Architecture

### Structure des Fichiers

```
frontend/src/styles/
├── design-system.scss          # Système de design principal (variables, mixins)
├── table-enhancements.scss    # Styles pour les tableaux PrimeNG
├── layout-enhancements.scss   # Styles pour le layout principal
├── form-enhancements.scss     # Styles pour les formulaires
├── cards-widgets.scss         # Styles pour les cartes et widgets
├── responsive.scss            # Utilitaires responsive
└── animations.scss            # Animations et transitions
```

### Composants UI

```
frontend/src/app/shared/components/
├── ui-card/                   # Composant Card réutilisable
├── ui-button/                 # Composant Button réutilisable
└── ui-badge/                  # Composant Badge réutilisable
```

---

## Palette de Couleurs

### Couleurs Primaires

| Variable | Valeur | Usage |
|----------|--------|-------|
| `$primary-500` | #6366F1 | Couleur principale |
| `$primary-600` | #4F46E5 | Hover state |
| `$primary-700` | #4338CA | Active state |

### Couleurs Sémantiques

| Type | Success | Warning | Error | Info |
|------|---------|---------|-------|------|
| Base | #10B981 | #F59E0B | #EF4444 | #3B82F6 |
| Hover | #059669 | #D97706 | #DC2626 | #2563EB |

### Couleurs Neutres

| Variable | Valeur | Usage |
|----------|--------|-------|
| `$neutral-0` | #FFFFFF | Arrière-plan principal |
| `$neutral-50` | #F8FAFC | Arrière-plan secondaire |
| `$neutral-900` | #0F172A | Texte principal |
| `$neutral-500` | #64748B | Texte secondaire |

### Gradients

```scss
$gradient-primary: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
$gradient-success: linear-gradient(135deg, #10B981 0%, #059669 100%);
$gradient-warning: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
$gradient-error: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
```

### Utilisation

```scss
// Dans les composants
.element {
  background: $gradient-primary;
  color: $color-text-primary;
  border-color: $color-border-light;
}

// En CSS (variables CSS)
.element {
  background: var(--gradient-primary);
  color: var(--color-text-primary);
}
```

---

## Typographie

### Familles de Polices

```scss
$font-family-base: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
$font-family-heading: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
$font-family-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

### Échelle Typographique

| Classe | Taille | Usage |
|--------|--------|-------|
| `$font-size-xs` | 12px | Labels, badges |
| `$font-size-sm` | 14px | Texte secondaire |
| `$font-size-base` | 16px | Texte par défaut |
| `$font-size-lg` | 18px | Sous-titres |
| `$font-size-xl` | 20px | Petits titres |
| `$font-size-2xl` | 24px | Titres de section |
| `$font-size-3xl` | 30px | Gros titres |
| `$font-size-4xl` | 36px | Hero titles |
| `$font-size-5xl` | 48px | Display titles |

### Poids des Polices

```scss
$font-weight-light: 300;
$font-weight-normal: 400;
$font-weight-medium: 500;
$font-weight-semibold: 600;
$font-weight-bold: 700;
$font-weight-extrabold: 800;
```

### Interlignage

```scss
$line-height-tight: 1.25;    // Titres
$line-height-normal: 1.5;    // Texte standard
$line-height-relaxed: 1.75;  // Texte long
```

### Utilisation

```scss
.heading {
  font-family: $font-family-heading;
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  line-height: $line-height-tight;
  letter-spacing: $letter-spacing-tight;
}

.body-text {
  font-family: $font-family-base;
  font-size: $font-size-base;
  font-weight: $font-weight-normal;
  line-height: $line-height-normal;
}
```

---

## Espacement

### Échelle d'Espacement

| Variable | Valeur | Usage |
|----------|--------|-------|
| `$spacing-1` | 4px | Micro espacements |
| `$spacing-2` | 8px | Très petit |
| `$spacing-3` | 12px | Petit |
| `$spacing-4` | 16px | Standard |
| `$spacing-5` | 20px | Moyen |
| `$spacing-6` | 24px | Grand |
| `$spacing-8` | 32px | Très grand |
| `$spacing-10` | 40px | Extra large |
| `$spacing-12` | 48px | Section spacing |
| `$spacing-16` | 64px | Page spacing |

### Utilisation

```scss
.card {
  padding: $spacing-6;
  margin-bottom: $spacing-4;
  gap: $spacing-3;
}
```

---

## Bordures et Coins Arrondis

### Rayons de Bordure

```scss
$radius-none: 0;
$radius-sm: 0.25rem;   // 4px
$radius-md: 0.375rem;  // 6px
$radius-lg: 0.5rem;    // 8px
$radius-xl: 0.75rem;   // 12px
$radius-2xl: 1rem;     // 16px
$radius-3xl: 1.5rem;   // 24px
$radius-full: 9999px;  // Cercle
```

### Utilisation

```scss
.button {
  border-radius: $radius-lg;
}

.card {
  border-radius: $radius-2xl;
}

.avatar {
  border-radius: $radius-full;
}
```

---

## Ombres

### Niveaux d'Ombre

```scss
$shadow-xs: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
$shadow-sm: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
$shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
$shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
$shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
$shadow-2xl: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
```

### Ombres Colorées

```scss
$shadow-colored-primary: 0 4px 14px 0 rgba(99, 102, 241, 0.39);
$shadow-colored-success: 0 4px 14px 0 rgba(16, 185, 129, 0.39);
$shadow-colored-warning: 0 4px 14px 0 rgba(245, 158, 11, 0.39);
$shadow-colored-error: 0 4px 14px 0 rgba(239, 68, 68, 0.39);
```

### Utilisation

```scss
.card {
  box-shadow: $shadow-md;

  &:hover {
    box-shadow: $shadow-xl;
  }
}

.button-primary {
  box-shadow: $shadow-colored-primary;
}
```

---

## Transitions et Animations

### Durées de Transition

```scss
$transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
$transition-base: 200ms cubic-bezier(0.4, 0, 0.2, 1);
$transition-slow: 300ms cubic-bezier(0.4, 0, 0.2, 1);
$transition-smooth: 400ms cubic-bezier(0.16, 1, 0.3, 1);
```

### Animations d'Entrée

| Classe | Description |
|--------|-------------|
| `.animate-fade-in-up` | Fade in avec mouvement vers le haut |
| `.animate-fade-in-down` | Fade in avec mouvement vers le bas |
| `.animate-scale-in` | Scale in |
| `.animate-slide-in-left` | Slide depuis la gauche |
| `.animate-slide-in-right` | Slide depuis la droite |
| `.animate-bounce-in` | Bounce in |

### Animations Continues

| Classe | Description |
|--------|-------------|
| `.animate-pulse` | Pulsation |
| `.animate-spin` | Rotation continue |
| `.animate-float` | Flottement |
| `.animate-bounce` | Rebond |

### Hover Effects

| Classe | Description |
|--------|-------------|
| `.hover-lift` | Élévation au hover |
| `.hover-scale` | Mise à l'échelle au hover |
| `.hover-glow` | Effet de lueur |
| `.hover-rotate` | Rotation légère |

### Utilisation

```scss
.card {
  transition: all $transition-smooth;

  &:hover {
    transform: translateY(-8px);
    box-shadow: $shadow-xl;
  }
}

// En HTML
<div class="card animate-fade-in-up hover-lift">
  Contenu
</div>
```

---

## Composants UI

### UiCard

Composant de carte réutilisable avec plusieurs variantes.

**Props:**
- `title?: string` - Titre de la carte
- `subtitle?: string` - Sous-titre
- `icon?: string` - Icône Bootstrap
- `variant?: 'default' | 'primary' | 'success' | 'warning' | 'error'`
- `hoverable?: boolean` - Effet au hover
- `clickable?: boolean` - Clic activé
- `loading?: boolean` - État de chargement
- `padding?: 'none' | 'sm' | 'md' | 'lg'`
- `shadow?: 'none' | 'sm' | 'md' | 'lg' | 'xl'`

**Exemple:**

```html
<app-ui-card
  title="Statistiques"
  subtitle="Vue d'ensemble"
  icon="bi-bar-chart"
  variant="primary"
  hoverable="true"
  padding="md">
  Contenu de la carte
</app-ui-card>
```

### UiButton

Composant de bouton polyvalent.

**Props:**
- `variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'ghost' | 'link'`
- `size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'`
- `icon?: string` - Icône Bootstrap
- `iconPosition?: 'left' | 'right'`
- `loading?: boolean`
- `disabled?: boolean`
- `fullWidth?: boolean`
- `rounded?: boolean`
- `outlined?: boolean`

**Exemple:**

```html
<button app-ui-button
  variant="primary"
  size="md"
  icon="bi-plus"
  iconPosition="left">
  Nouveau
</button>
```

### UiBadge

Composant de badge pour les états et labels.

**Props:**
- `variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info' | 'neutral'`
- `size?: 'xs' | 'sm' | 'md' | 'lg'`
- `icon?: string`
- `outlined?: boolean`
- `dot?: boolean`

**Exemple:**

```html
<app-ui-badge variant="success" size="sm">Actif</app-ui-badge>
<app-ui-badge variant="error" size="md" dot></app-ui-badge>
```

---

## Responsive Design

### Breakpoints

```scss
$breakpoint-xs: 0;
$breakpoint-sm: 640px;
$breakpoint-md: 768px;
$breakpoint-lg: 1024px;
$breakpoint-xl: 1280px;
$breakpoint-2xl: 1536px;
```

### Mixins Responsive

```scss
// Mobile First
@include respond-to('sm') { /* ≥ 640px */ }
@include respond-to('md') { /* ≥ 768px */ }
@include respond-to('lg') { /* ≥ 1024px */ }
@include respond-to('xl') { /* ≥ 1280px */ }

// Desktop First
@include respond-below('md') { /* < 768px */ }
@include respond-below('lg') { /* < 1024px */ }
```

### Grid System

```scss
.responsive-grid {
  display: grid;
  gap: $spacing-6;

  &--cols-2 {
    grid-template-columns: repeat(2, 1fr);
    @include respond-below('md') {
      grid-template-columns: 1fr;
    }
  }

  &--auto {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}
```

### Utilitaires

| Classe | Description |
|--------|-------------|
| `.hide-mobile` | Masqué sur mobile |
| `.hide-desktop` | Masqué sur desktop |
| `.show-mobile-only` | Visible uniquement sur mobile |
| `.touch-target` | Zone de tactile optimisée (44px min) |

---

## Dark Mode

### Activation

Le dark mode est activé via la classe `.my-app-dark` sur l'élément `<body>`.

```html
<body class="my-app-dark">
  <!-- Contenu -->
</body>
```

### Variables CSS

Les variables CSS sont automatiquement mises à jour en dark mode:

```scss
.my-app-dark {
  --color-text-primary: #{$neutral-100};
  --color-text-secondary: #{$neutral-400};
  --color-background-primary: #{$neutral-900};
  --color-background-secondary: #{$neutral-800};
  // ... autres variables
}
```

### Implémentation dans les Composants

```scss
.card {
  background: $glass-bg-light;
  border-color: $glass-border-light;

  // Dark mode override
  .my-app-dark & {
    background: $glass-bg-dark;
    border-color: $glass-border-dark;
  }
}
```

---

## Bonnes Pratiques

### 1. Utiliser les Variables

```scss
// ✅ Bon
.element {
  padding: $spacing-4;
  border-radius: $radius-lg;
  color: $color-text-primary;
}

// ❌ Mauvais
.element {
  padding: 16px;
  border-radius: 8px;
  color: #1E293B;
}
```

### 2. Utiliser les Mixins

```scss
// ✅ Bon
.card {
  @include card-style();
  @include flex-center;
}

// ❌ Mauvais
.card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}
```

### 3. Accessibility

```scss
// Toujours inclure un focus visible
.button:focus-visible {
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.3);
  outline: none;
}

// Respecter les préférences utilisateur
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 4. Performance

```scss
// Utiliser transform et opacity pour les animations
.card {
  transition: transform $transition-smooth, opacity $transition-smooth;
  
  &:hover {
    transform: translateY(-8px);
    opacity: 0.95;
  }
}

// Éviter de changer layout avec animations
// ❌ Mauvais
.card {
  transition: height $transition-smooth, width $transition-smooth;
}
```

### 5. Nommage

```scss
// Utiliser BEM ou une convention cohérente
.card { }
.card__header { }
.card__content { }
.card--primary { }
.card--hoverable { }
```

### 6. Ordre des Propriétés

```scss
.element {
  // 1. Position
  position: relative;
  z-index: 1;

  // 2. Box Model
  display: flex;
  width: 100%;
  height: auto;
  padding: $spacing-4;
  margin: $spacing-2;

  // 3. Typography
  font-size: $font-size-base;
  font-weight: $font-weight-normal;
  line-height: $line-height-normal;
  color: $color-text-primary;

  // 4. Visual
  background: $color-background-primary;
  border: 1px solid $color-border-medium;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;

  // 5. Animation
  transition: all $transition-base;

  // 6. Misc
  cursor: pointer;
}
```

---

## Intégration

### Import des Styles

Dans `styles.scss`:

```scss
@import './styles/design-system.scss';
@import './styles/table-enhancements.scss';
@import './styles/layout-enhancements.scss';
@import './styles/form-enhancements.scss';
@import './styles/cards-widgets.scss';
@import './styles/responsive.scss';
@import './styles/animations.scss';
```

### Utilisation des Composants

Dans un module Angular:

```typescript
import { UiCardComponent } from './shared/components/ui-card/ui-card.component';
import { UiButtonComponent } from './shared/components/ui-button/ui-button.component';
import { UiBadgeComponent } from './shared/components/ui-badge/ui-badge.component';

@Component({
  standalone: true,
  imports: [UiCardComponent, UiButtonComponent, UiBadgeComponent],
  // ...
})
export class MyComponent { }
```

---

## Ressources

### Liens Utiles

- [PrimeNG Documentation](https://primeng.org/)
- [Bootstrap Documentation](https://getbootstrap.com/)
- [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/WCAG21/quickref/)
- [Design Systems Gallery](https://designsystemsrepo.com/)

### Outils

- [Coolors](https://coolors.co/) - Générateur de palettes
- [Figma](https://www.figma.com/) - Design prototypage
- [Chrome DevTools](https://developers.google.com/web/tools/chrome-devtools) - Debug CSS

---

## Changelog

### Version 2.0.0 (2024)

- ✨ Création du design system complet
- ✨ Ajout des composants UI réutilisables
- ✨ Implémentation du dark mode
- ✨ Système responsive complet
- ✨ Animations et transitions avancées
- 📚 Documentation complète

---

## Support

Pour toute question ou suggestion concernant le design system, contactez l'équipe de développement frontend.

**Mainteneur:** Équipe Frontend Vermeg Assurance  
**Licence:** Interne

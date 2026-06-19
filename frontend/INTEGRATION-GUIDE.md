# Guide d'Intégration Rapide - Design System

## 🚀 Installation

Le design system est déjà configuré dans votre projet. Voici comment l'utiliser.

## 📋 Prérequis

Assurez-vous d'avoir les dépendances suivantes installées :

```bash
npm install bootstrap primeng primeicons @angular/animations
```

## 🎨 Utilisation des Styles

Les styles sont déjà importés dans `src/styles.scss` et configurés dans `angular.json`. Aucune configuration supplémentaire n'est nécessaire.

## 🧩 Utilisation des Composants UI

### 1. Importer les composants dans votre module

```typescript
import { UiCardComponent } from './shared/components/ui-card/ui-card.component';
import { UiButtonComponent } from './shared/components/ui-button/ui-button.component';
import { UiBadgeComponent } from './shared/components/ui-badge/ui-badge.component';

@Component({
  standalone: true,
  imports: [
    UiCardComponent,
    UiButtonComponent,
    UiBadgeComponent
    // ... autres imports
  ],
  // ...
})
export class MyComponent { }
```

### 2. Utiliser les composants dans le template

#### Card Component
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

#### Button Component
```html
<button app-ui-button
  variant="primary"
  size="md"
  icon="bi-plus"
  iconPosition="left"
  (buttonClick)="onButtonClick()">
  Nouveau
</button>
```

#### Badge Component
```html
<app-ui-badge variant="success" size="sm">Actif</app-ui-badge>
<app-ui-badge variant="error" size="md" dot></app-ui-badge>
```

## 🎯 Utilisation des Classes CSS

### Animations
```html
<div class="animate-fade-in-up">Contenu animé</div>
<div class="hover-lift">Élévation au hover</div>
<div class="transition-all">Transition fluide</div>
```

### Layout
```html
<div class="container">Conteneur centré</div>
<div class="responsive-grid responsive-grid--cols-3">Grille responsive</div>
<div class="flex-responsive">Flexbox responsive</div>
```

### Forms
```html
<div class="form-container">
  <div class="form-group">
    <label class="form-label">Label</label>
    <input type="text" class="form-control" placeholder="...">
  </div>
</div>
```

## 🌙 Activer le Dark Mode

Ajoutez la classe `my-app-dark` au body :

```typescript
// Dans votre composant principal
toggleDarkMode() {
  document.body.classList.toggle('my-app-dark');
}
```

Ou utilisez un service pour gérer le thème :

```typescript
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private isDarkMode = false;

  toggleTheme() {
    this.isDarkMode = !this.isDarkMode;
    if (this.isDarkMode) {
      document.body.classList.add('my-app-dark');
    } else {
      document.body.classList.remove('my-app-dark');
    }
  }
}
```

## 📱 Responsive Design

Le design system inclut des utilitaires responsive :

```scss
// Dans vos composants
@include respond-below('md') {
  // Styles pour mobile/tablette
}

@include respond-to('lg') {
  // Styles pour desktop et plus
}
```

## 🔧 Personnalisation

Vous pouvez personnaliser les variables CSS dans `styles/design-system.scss` :

```scss
// Modifier les couleurs
$color-primary: #custom-color;

// Modifier l'espacement
$spacing-4: 20px; // au lieu de 16px

// Modifier les breakpoints
$breakpoint-md: 768px;
```

## 📚 Documentation Complète

Pour plus de détails, consultez le fichier `DESIGN-SYSTEM.md`.

## 🐛 Dépannage

### Les styles ne s'appliquent pas
1. Vérifiez que `styles.scss` est bien importé dans `angular.json`
2. Redémarrez le serveur de développement : `ng serve`
3. Videz le cache du navigateur

### Les composants UI ne fonctionnent pas
1. Vérifiez que les composants sont bien importés dans votre module
2. Assurez-vous que le composant est marqué comme `standalone: true`

### Le dark mode ne fonctionne pas
1. Vérifiez que la classe `my-app-dark` est ajoutée au body
2. Vérifiez les overrides dark mode dans les fichiers SCSS

## ✅ Checklist d'Intégration

- [ ] Vérifier que `styles.scss` est dans `angular.json`
- [ ] Importer les composants UI nécessaires
- [ ] Tester les animations et transitions
- [ ] Tester le responsive design
- [ ] Tester le dark mode
- [ ] Consulter la documentation complète

## 🎓 Exemples d'Utilisation

### Exemple 1: Dashboard Card
```html
<app-ui-card
  title="Utilisateurs"
  subtitle="Total des utilisateurs"
  icon="bi-people"
  variant="primary"
  hoverable="true">
  <div class="stat-value">1,234</div>
  <div class="stat-trend stat-trend--up">
    <i class="bi bi-arrow-up"></i> +12%
  </div>
</app-ui-card>
```

### Exemple 2: Form Group
```html
<div class="form-group">
  <label class="form-label form-label--required">Email</label>
  <input type="email" 
         class="form-control" 
         placeholder="votre@email.com"
         [class.form-control--valid]="email.valid">
  <div class="form-feedback form-feedback--invalid" *ngIf="email.invalid">
    Email invalide
  </div>
</div>
```

### Exemple 3: Table Styling
```html
<div class="responsive-table">
  <p-table [value]="data" class="p-table">
    <!-- Table content -->
  </p-table>
</div>
```

## 📞 Support

Pour toute question ou problème, consultez la documentation complète dans `DESIGN-SYSTEM.md`.

import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout.component')
        .then(m => m.MainLayoutComponent),

    canActivate: [authGuard],

    children: [

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component')
            .then(m => m.DashboardComponent),
        title: 'Dashboard'
      },

      {
        path: 'users',
        loadComponent: () =>
          import('./pages/users/users.component')
            .then(m => m.UsersComponent),
        title: 'Gestion des utilisateurs'
      },

      {
        path: 'produits',
        loadComponent: () =>
          import('./pages/produits/produits.component')
            .then(m => m.ProduitsComponent),
        title: 'Gestion des produits'
      },

      {
        path: 'produits/add',
        loadComponent: () =>
          import('./pages/produits/produit-form.component')
            .then(m => m.ProduitFormComponent),
        title: 'Ajouter un produit'
      },

      {
        path: 'produits/edit/:idProduit',
        loadComponent: () =>
          import('./pages/produits/produit-form.component')
            .then(m => m.ProduitFormComponent),
        title: 'Modifier un produit'
      },

      {
        path: 'produits/:idProduit',
        loadComponent: () =>
          import('./pages/produits/produit-details.component')
            .then(m => m.ProduitDetailsComponent),
        title: 'Détails produit'
      },

      {
        path: 'packs',
        loadComponent: () =>
          import('./pages/packs/packs.component')
            .then(m => m.PacksComponent),
        title: 'Gestion des packs'
      },

      {
        path: 'packs/add',
        loadComponent: () =>
          import('./pages/packs/pack-form.component')
            .then(m => m.PackFormComponent),
        title: 'Ajouter un pack'
      },

      {
        path: 'packs/edit/:idPack',
        loadComponent: () =>
          import('./pages/packs/pack-form.component')
            .then(m => m.PackFormComponent),
        title: 'Modifier un pack'
      },

      {
        path: 'packs/:idPack',
        loadComponent: () =>
          import('./pages/packs/pack-details.component')
            .then(m => m.PackDetailsComponent),
        title: 'Détails pack'
      },

      {
        path: 'garanties',
        loadComponent: () =>
          import('./pages/garanties/garanties.component')
            .then(m => m.GarantiesComponent),
        title: 'Gestion des garanties'
      },

      {
        path: 'garanties/add',
        loadComponent: () =>
          import('./pages/garanties/garantie-form.component')
            .then(m => m.GarantieFormComponent),
        title: 'Ajouter une garantie'
      },

      {
        path: 'garanties/edit/:idGarantie',
        loadComponent: () =>
          import('./pages/garanties/garantie-form.component')
            .then(m => m.GarantieFormComponent),
        title: 'Modifier une garantie'
      },

      {
        path: 'garanties/:idGarantie',
        loadComponent: () =>
          import('./pages/garanties/garantie-details.component')
            .then(m => m.GarantieDetailsComponent),
        title: 'Détails garantie'
      },

      {
        path: 'chatbot',
        loadComponent: () =>
          import('./pages/unified-chatbot/unified-chatbot.component')
            .then(m => m.UnifiedChatbotComponent),
        title: 'Assistant IA Assurance'
      },

      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  {
    path: '**',
    redirectTo: ''
  }
];

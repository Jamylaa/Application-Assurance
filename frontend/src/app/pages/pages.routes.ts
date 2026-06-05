import { Routes } from '@angular/router';
import { MainLayoutComponent } from '../layout/main-layout.component';
import { UsersComponent } from './users/users.component';
import { ProduitsComponent } from './produits/produits.component';
import { PacksComponent } from './packs/packs.component';
import { GarantiesComponent } from './garanties/garanties.component';

export const PAGES_ROUTES: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: 'users',
        component: UsersComponent,
        title: 'Gestion des utilisateurs'
      },
      {
        path: 'produits',
        component: ProduitsComponent,
        title: 'Gestion des produits'
      },
      {
        path: 'packs',
        component: PacksComponent,
        title: 'Gestion des packs'
      },
      {
        path: 'garanties',
        component: GarantiesComponent,
        title: 'Gestion des garanties'
      },
      {
        path: '',
        redirectTo: 'users',
        pathMatch: 'full'
      }
    ]
  }
];

import { Routes } from '@angular/router';
import { AuthLayoutComponent } from '../layout/auth-layout.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      {
        path: 'Connexion',
        component: LoginComponent,
        title: 'Se connecter - Plateforme d\'Assurance'
      },
      {
        path: 'Inscription',
        component: RegisterComponent,
        title: 'S\'inscrire - Plateforme d\'Assurance'
      },
      {
        path: '',
        redirectTo: 'Connexion',
        pathMatch: 'full'
      }
    ]
  }
];

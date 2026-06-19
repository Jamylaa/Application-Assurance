import { Component } from '@angular/core';

import { RouterModule } from '@angular/router';

import {
  ButtonModule
} from 'primeng/button';

import {
  CommonModule
} from '@angular/common';

import {
  KeycloakService
} from 'keycloak-angular';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,

  imports: [
    ButtonModule,
    CommonModule,
    RouterModule
  ]
})

export class LoginComponent {

  loading = false;

  constructor(
    private readonly keycloak: KeycloakService
  ) {}


  async login(): Promise<void> {

    this.loading = true;

    await this.keycloak.login({
      redirectUri: window.location.origin
    });
  }
}
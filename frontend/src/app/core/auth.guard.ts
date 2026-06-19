import {CanActivateFn} from '@angular/router';
import { inject } from '@angular/core';
import { KeycloakService} from 'keycloak-angular';

export const authGuard: CanActivateFn = async () => {

  const keycloak = inject(KeycloakService);

  return await keycloak.isLoggedIn();
};
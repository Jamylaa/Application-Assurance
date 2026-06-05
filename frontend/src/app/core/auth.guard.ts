import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  if (await keycloak.isLoggedIn()) {
    return true;
  }
  await keycloak.login({
    redirectUri: window.location.origin
  });
  return false;
};

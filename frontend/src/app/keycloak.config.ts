import { KeycloakService } from 'keycloak-angular';
import { environment } from '../environments/environment';

export function initializeKeycloak(
  keycloak: KeycloakService
) {
  return () =>
    keycloak.init({
      config: {
        url: environment.keycloakUrl || 'http://localhost:9090',
        realm: 'vermeg-realm',
        clientId: 'frontend-client'
      },
      initOptions: {
        onLoad: 'login-required',
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
      }
    });
}
import {
  ApplicationConfig,
  APP_INITIALIZER,
  inject
} from '@angular/core';

import {
  provideRouter,
  Router
} from '@angular/router';

import {
  provideHttpClient,
  withInterceptors,
  HttpInterceptorFn
} from '@angular/common/http';

import { provideAnimations } from '@angular/platform-browser/animations';

import {
  ConfirmationService,
  MessageService
} from 'primeng/api';

import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

import { KeycloakService } from 'keycloak-angular';

import { routes } from './app.routes';

import { authInterceptor } from './core/auth.interceptor';

import { initializeKeycloak } from './keycloak.config';


// ERROR INTERCEPTOR
const errorInterceptor: HttpInterceptorFn = (
  req,
  next
) => {

  const router = inject(Router);

  return next(req).pipe(

    catchError((error: any) => {

      if (error.status === 401) {

        router.navigate(['/']);
      }

      return throwError(() => error);
    })
  );
};


// APP CONFIG
export const appConfig: ApplicationConfig = {

  providers: [

    provideRouter(routes),

    provideHttpClient(
      withInterceptors([
        authInterceptor,
        errorInterceptor
      ])
    ),

    provideAnimations(),

    ConfirmationService,
    MessageService,

    KeycloakService,

    {
      provide: APP_INITIALIZER,

      useFactory: initializeKeycloak,

      multi: true,

      deps: [KeycloakService]
    }
  ]
};
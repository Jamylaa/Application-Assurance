import { HttpInterceptorFn } from '@angular/common/http';

import { inject } from '@angular/core';

import { KeycloakService} from 'keycloak-angular';

import { from} from 'rxjs';

import {switchMap}from 'rxjs/operators';

export const authInterceptor: HttpInterceptorFn = (  req,next) => {

  const keycloak = inject(KeycloakService);

  return from(
    keycloak.getToken()
  ).pipe(

    switchMap((token) => {

      if (token) {

        req = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
      }

      return next(req);
    })
  );
};
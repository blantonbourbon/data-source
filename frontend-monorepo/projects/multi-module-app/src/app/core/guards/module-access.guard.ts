import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, catchError, map, of } from 'rxjs';

import { AuthorizationService } from '../services/authorization.service';
import { ConfigService } from '../services/config.service';

@Injectable({
  providedIn: 'root'
})
export class ModuleAccessGuard implements CanActivate {
  constructor(
    private readonly router: Router,
    private readonly configService: ConfigService,
    private readonly authorizationService: AuthorizationService
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    const moduleId = route.paramMap.get('moduleId');
    if (!moduleId) {
      return of(true);
    }

    return this.ensureModuleAvailable(moduleId).pipe(
      map(() => {
        const moduleDefinition = this.configService.getModuleById(moduleId);
        if (!moduleDefinition) {
          return this.router.createUrlTree(['/']);
        }

        if (this.authorizationService.hasModuleAccess(moduleDefinition)) {
          return true;
        }

        return this.router.createUrlTree(['/access-denied'], {
          queryParams: {
            module: moduleId,
            from: state.url
          }
        });
      }),
      catchError(() =>
        of(
          this.router.createUrlTree(['/access-denied'], {
            queryParams: {
              module: moduleId,
              from: state.url
            }
          })
        )
      )
    );
  }

  private ensureModuleAvailable(moduleId: string): Observable<void> {
    if (!moduleId.startsWith('ext-')) {
      return of(void 0);
    }

    return this.configService.loadExternalApps().pipe(
      map(() => void 0),
      catchError(() => of(void 0))
    );
  }
}

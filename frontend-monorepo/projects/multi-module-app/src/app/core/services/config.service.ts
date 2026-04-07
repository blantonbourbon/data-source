import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, shareReplay, tap, throwError } from 'rxjs';

import { ModuleDefinition, ModuleAuthorization } from '../models/auth.models';
import { BuiltinModules, DataQueryConfig } from '../config/data-query.config';

export interface ExternalAppConfig extends ModuleDefinition {
  id: string;
  name: string;
  description: string;
  url: string;
  bgStyle: string;
  logo: string;
  authorization?: ModuleAuthorization;
}

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private externalApps: ExternalAppConfig[] = [];
  private externalAppsRequest$?: Observable<ExternalAppConfig[]>;

  constructor(private http: HttpClient) {}

  getInternalModules(): DataQueryConfig[] {
    return Object.values(BuiltinModules);
  }

  getInternalModuleById(id: string): DataQueryConfig | undefined {
    return BuiltinModules[id];
  }

  loadExternalApps(): Observable<ExternalAppConfig[]> {
    if (this.externalApps.length > 0) {
      return of(this.externalApps);
    }

    if (!this.externalAppsRequest$) {
      this.externalAppsRequest$ = this.http.get<ExternalAppConfig[]>('/external-apps.json').pipe(
        tap(apps => (this.externalApps = apps)),
        catchError(error => {
          this.externalAppsRequest$ = undefined;
          return throwError(() => error);
        }),
        shareReplay(1)
      );
    }

    return this.externalAppsRequest$;
  }

  getExternalApps(): ExternalAppConfig[] {
    return this.externalApps;
  }

  getExternalAppById(id: string): ExternalAppConfig | undefined {
    return (
      this.externalApps.find(app => id.startsWith('ext-') && app.id === id.substring(4)) ||
      this.externalApps.find(app => app.id === id)
    );
  }

  getModuleById(id: string): ModuleDefinition | undefined {
    return this.getInternalModuleById(id) || this.getExternalAppById(id);
  }
}

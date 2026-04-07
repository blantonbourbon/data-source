import { Injectable } from '@angular/core';

import { ModuleAuthorization, ModuleDefinition } from '../models/auth.models';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {
  constructor(private readonly authService: AuthService) {}

  hasModuleAccess(moduleDefinition: Pick<ModuleDefinition, 'authorization'> | undefined): boolean {
    return this.hasAuthorization(moduleDefinition?.authorization);
  }

  hasAuthorization(authorization: ModuleAuthorization | undefined): boolean {
    const requiredPermissions = this.normalizeList(authorization?.permissions);

    if (requiredPermissions.length === 0) {
      return !this.hasUnsupportedRules(authorization);
    }

    const currentUser = this.authService.currentUserValue;
    if (!currentUser) {
      return false;
    }

    const userPermissions = new Set(this.normalizeList(currentUser.permissions));
    const matchMode = authorization?.match ?? 'all';

    if (matchMode === 'any') {
      return requiredPermissions.some(permission => userPermissions.has(permission));
    }

    return requiredPermissions.every(permission => userPermissions.has(permission));
  }

  filterAuthorizedModuleIds<T extends Pick<ModuleDefinition, 'authorization'>>(
    moduleIds: string[],
    resolver: (moduleId: string) => T | undefined
  ): string[] {
    return moduleIds.filter(moduleId => this.hasModuleAccess(resolver(moduleId)));
  }

  private normalizeList(values: string[] | undefined): string[] {
    return (values ?? []).map(value => value.trim().toLowerCase()).filter(value => value.length > 0);
  }

  private hasUnsupportedRules(authorization: ModuleAuthorization | undefined): boolean {
    if (!authorization || typeof authorization !== 'object') {
      return false;
    }

    return Object.entries(authorization as Record<string, unknown>).some(([key, value]) => {
      if (key === 'permissions' || key === 'match') {
        return false;
      }

      return Array.isArray(value) ? value.length > 0 : !!value;
    });
  }
}

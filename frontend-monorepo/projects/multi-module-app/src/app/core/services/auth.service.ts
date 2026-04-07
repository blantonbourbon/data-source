import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';

import { AuthConfig, AuthUser, BackendUserContext } from '../models/auth.models';
import { AuthConfigService } from './auth-config.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly httpBackend = inject(HttpBackend);
  private readonly authConfigService = inject(AuthConfigService);
  private readonly authHttp = new HttpClient(this.httpBackend);

  private readonly currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  readonly currentUser$ = this.currentUserSubject.asObservable();

  async initialize(): Promise<void> {
    const authConfig = await firstValueFrom(this.authConfigService.loadConfig());
    await this.loadCurrentUser(authConfig);
  }

  async login(returnUrl?: string): Promise<void> {
    const authConfig = await firstValueFrom(this.authConfigService.loadConfig());
    const nextReturnUrl = this.normalizeReturnUrl(returnUrl || authConfig.defaultReturnUrl);
    this.redirectBrowser(this.appendReturnUrl(authConfig.loginPath, nextReturnUrl));
  }

  logout(): void {
    this.clearCurrentUser();
    this.redirectBrowser(this.getOptionalConfig()?.logoutPath || '/api/auth/logout');
  }

  isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  get currentUserValue(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  private async loadCurrentUser(authConfig: AuthConfig): Promise<void> {
    try {
      const backendUserContext = await firstValueFrom(this.authHttp.get<BackendUserContext>(authConfig.mePath));
      this.currentUserSubject.next(this.mapBackendUserContext(backendUserContext));
    } catch (error) {
      this.clearCurrentUser();

      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        console.error('Failed to load current user context', error);
      }
    }
  }

  private mapBackendUserContext(userContext: BackendUserContext): AuthUser {
    const permissions = this.uniqueList(this.normalizeList(userContext.permissions));
    const username = this.firstDefinedValue(userContext.username, userContext.email, userContext.id) || 'unknown-user';

    return {
      id: this.firstDefinedValue(userContext.id, username) || username,
      username,
      displayName: this.firstDefinedValue(userContext.displayName, username) || username,
      email: this.normalizeString(userContext.email),
      permissions,
      dataScopes: userContext.dataScopes ?? {},
      claims: userContext.claims ?? {}
    };
  }

  private appendReturnUrl(path: string, returnUrl: string): string {
    const query = new URLSearchParams({ returnUrl });
    const separator = path.includes('?') ? '&' : '?';
    return `${path}${separator}${query.toString()}`;
  }

  private normalizeReturnUrl(returnUrl: string | undefined): string {
    return this.normalizeString(returnUrl) || '/';
  }

  private normalizeString(value: string | undefined): string | undefined {
    const trimmedValue = value?.trim();
    return trimmedValue ? trimmedValue : undefined;
  }

  private normalizeList(values: string[] | undefined): string[] {
    return (values ?? []).map(value => value.trim()).filter(value => value.length > 0);
  }

  private uniqueList(values: string[]): string[] {
    return Array.from(new Set(values.map(value => value.trim()).filter(value => value.length > 0)));
  }

  private firstDefinedValue(...values: Array<string | undefined>): string | undefined {
    return values.map(value => this.normalizeString(value)).find((value): value is string => !!value);
  }

  private clearCurrentUser(): void {
    this.currentUserSubject.next(null);
  }

  private redirectBrowser(url: string): void {
    window.location.assign(url);
  }

  private getOptionalConfig(): AuthConfig | undefined {
    try {
      return this.authConfigService.getConfig();
    } catch {
      return undefined;
    }
  }
}

# Frontend OIDC Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Angular standalone frontend that authenticates users via the backend's OIDC flow (PingFederate), using the existing `/api/auth/login`, `/api/me`, and `/api/auth/logout` endpoints, and consumes the audience-scoped `/api/user/{entity}/**` REST APIs.

**Architecture:** The backend already handles the full OAuth2 authorization code flow server-side (session-based, cookie-driven). The frontend does NOT need an OIDC client library. Instead it simply redirects unauthenticated users to `/api/auth/login?returnUrl=<current-path>`, reads session state from `GET /api/me`, and logs out via `GET /api/auth/logout`. All data API calls go to `/api/user/{entity}/**` with cookies.

**Tech Stack:** Angular 19 standalone (no NX), Angular CLI, Angular Router, Angular HttpClient, SCSS

---

## Backend Review Summary

Before the frontend plan, here is a brief review of the backend auth implementation:

### What's Good

1. **Clean dual filter chain separation** - `SecurityConfiguration.java` properly isolates Grafana mTLS (`/api/grafana/**`, Order 1) from user OIDC (`/api/user/**`, `/api/me`, `/api/auth/**`, Order 2). The order is correct: more specific chain first.
2. **mTLS handling is pragmatic** - `server.ssl.client-auth=want` allows both certificate-bearing and non-certificate requests on the same port, with application-layer enforcement in the Grafana chain. This is the right trade-off before needing a dedicated listener.
3. **Session security** - `SameSite=strict`, `HttpOnly=true`, CSRF disabled only for API paths (acceptable for session+cookie SPA pattern).
4. **ApplicationClientPrincipal / UserDetailsService** - Clean mapping from certificate CN to configurable application clients with authorities.
5. **BackendUserContextMapper** - Solid claim extraction with proper null handling and normalization.
6. **ReturnUrlAuthenticationSuccessHandler** - Safe redirect with open-redirect protection (requires `/` prefix, rejects `//`).
7. **Route splitting** - `DynamicRestController` as abstract base with `UserEntityController` (`/api/user/{entity}`) and `GrafanaEntityController` (`/api/grafana/{entity}`) is clean DRY.
8. **Tests** - `SecurityIntegrationTest` covers the critical paths: login redirect, 401 for unauthenticated, `/api/me` with mock OAuth2, Grafana mTLS accept/reject, cross-audience isolation, logout.

### Minor Observations (Not Blockers)

1. **No default security chain** - Paths not matched by either chain (e.g., static assets, `/**`) are unprotected by Spring Security. This is intentional if the frontend is served from a CDN/separate host, but if the Angular SPA is served from the same Spring Boot app, you may want a catch-all chain or a `WebMvcConfigurer` for static resources.
2. **CSRF fully disabled for user API** - This is fine for a session+cookie SPA where all mutations go through `POST`/`DELETE` to `/api/user/**`, but if you later add form-based or browser-navigated mutation endpoints, revisit.
3. **`GenericController<M, E>` vs `DynamicRestController`** - Both exist but serve different purposes (type-safe vs dynamic). Not a problem, just note `GenericController` is unused by the current route split.

---

## File Structure

```
dashboards/                          # Frontend root (Angular standalone, no NX)
  angular.json                       # Angular CLI workspace config
  package.json                       # Dependencies
  tsconfig.json                      # Root TypeScript config
  tsconfig.app.json                  # App-specific TS config
  tsconfig.spec.json                 # Test TS config
  src/
    main.ts                          # Bootstrap entry
    index.html                       # HTML shell
    styles.scss                      # Global styles
    app/
      app.ts                         # Root component
      app.config.ts                  # App providers (router, http)
      app.routes.ts                  # Route definitions
      core/
        auth.service.ts              # Auth state: /api/me, login redirect, logout
        auth.guard.ts                # Route guard: redirect to login if unauthenticated
        auth.interceptor.ts          # HTTP interceptor: handle 401 globally
        auth.model.ts                # BackendUserContext interface (matches backend DTO)
      features/
        home/
          home.component.ts          # Dashboard landing page
        login/
          login-redirect.component.ts  # Triggers /api/auth/login redirect
      shared/
        layout/
          layout.component.ts        # App shell with nav + user info + logout
```

The backend does not need changes. The existing endpoints are:
- `GET /api/auth/login?returnUrl=<path>` - redirects to PingFederate
- `GET /api/auth/logout` - invalidates session, redirects
- `GET /api/me` - returns `BackendUserContext` JSON
- `/api/user/{entity}/**` - CRUD for authenticated users

---

### Task 1: Scaffold Angular Project

**Files:**
- Create: `dashboards/` (Angular CLI workspace)

- [ ] **Step 1: Generate Angular project**

```bash
cd /d/projects/panel
ng new dashboards --standalone --style=scss --routing --skip-git --skip-tests --directory=dashboards
```

If `ng` is not installed globally:
```bash
npx @angular/cli@19 new dashboards --standalone --style=scss --routing --skip-git --skip-tests --directory=dashboards
```

- [ ] **Step 2: Verify project builds**

Run: `cd dashboards && npm start`
Expected: Angular dev server starts on http://localhost:4200

- [ ] **Step 3: Configure proxy for backend API**

Create: `dashboards/proxy.conf.json`

```json
{
  "/api": {
    "target": "https://localhost:8443",
    "secure": false,
    "changeOrigin": true
  },
  "/oauth2": {
    "target": "https://localhost:8443",
    "secure": false,
    "changeOrigin": true
  },
  "/login": {
    "target": "https://localhost:8443",
    "secure": false,
    "changeOrigin": true
  }
}
```

Then update `dashboards/angular.json` to add the proxy config to the `serve` target:

```json
"serve": {
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add dashboards/
git commit -m "feat: scaffold Angular 19 standalone frontend project"
```

---

### Task 2: Auth Model and Service

**Files:**
- Create: `dashboards/src/app/core/auth.model.ts`
- Create: `dashboards/src/app/core/auth.service.ts`

- [ ] **Step 1: Create the auth model**

Create: `dashboards/src/app/core/auth.model.ts`

```typescript
export interface BackendUserContext {
  id: string;
  username: string;
  displayName: string;
  email: string;
  groups: string[];
  entitlements: string[];
  permissions: string[];
  dataScopes: Record<string, unknown>;
  claims: Record<string, unknown>;
}
```

This mirrors the backend `BackendUserContext` record exactly.

- [ ] **Step 2: Create the auth service**

Create: `dashboards/src/app/core/auth.service.ts`

```typescript
import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BackendUserContext } from './auth.model';
import { catchError, of, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _user = signal<BackendUserContext | null>(null);
  private readonly _loading = signal(true);
  private readonly _checked = signal(false);

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => !!this._user());
  readonly loading = this._loading.asReadonly();
  readonly checked = this._checked.asReadonly();

  constructor(private http: HttpClient) {}

  /**
   * Call once at app init. Tries GET /api/me.
   * If 200 -> user is authenticated (session cookie valid).
   * If 401 -> user is not authenticated.
   */
  checkSession() {
    return this.http.get<BackendUserContext>('/api/me').pipe(
      tap(user => {
        this._user.set(user);
        this._loading.set(false);
        this._checked.set(true);
      }),
      catchError(() => {
        this._user.set(null);
        this._loading.set(false);
        this._checked.set(true);
        return of(null);
      })
    );
  }

  /**
   * Redirect the browser to the backend login endpoint.
   * The backend will redirect to PingFederate, and after auth
   * will redirect back to returnUrl.
   */
  login(returnUrl: string = '/'): void {
    window.location.href = `/api/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`;
  }

  /**
   * Redirect the browser to the backend logout endpoint.
   * The backend will invalidate the session and redirect.
   */
  logout(): void {
    window.location.href = '/api/auth/logout';
  }

  hasGroup(group: string): boolean {
    return this._user()?.groups.includes(group) ?? false;
  }

  hasPermission(permission: string): boolean {
    return this._user()?.permissions.includes(permission) ?? false;
  }

  hasEntitlement(entitlement: string): boolean {
    return this._user()?.entitlements.includes(entitlement) ?? false;
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add dashboards/src/app/core/
git commit -m "feat: add auth model and service for session-based OIDC"
```

---

### Task 3: Auth Guard and HTTP Interceptor

**Files:**
- Create: `dashboards/src/app/core/auth.guard.ts`
- Create: `dashboards/src/app/core/auth.interceptor.ts`

- [ ] **Step 1: Create the auth guard**

Create: `dashboards/src/app/core/auth.guard.ts`

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.checked()) {
    // Session hasn't been checked yet — shouldn't happen if APP_INITIALIZER is set up,
    // but redirect to login as fallback.
    authService.login(state.url);
    return false;
  }

  if (authService.isAuthenticated()) {
    return true;
  }

  // Not authenticated: redirect browser to backend login with return URL
  authService.login(state.url);
  return false;
};
```

- [ ] **Step 2: Create the HTTP interceptor**

Create: `dashboards/src/app/core/auth.interceptor.ts`

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    // We could add tap/catchError here to handle 401 globally.
    // For now, the guard handles auth checks before API calls.
    // If a 401 happens mid-session (session expired), we redirect to login.
  );
};
```

Note: The interceptor is intentionally minimal. The main auth flow is handled by server-side redirects and the guard. A global 401 handler can be added later.

- [ ] **Step 3: Commit**

```bash
git add dashboards/src/app/core/
git commit -m "feat: add auth guard and HTTP interceptor"
```

---

### Task 4: App Config with Providers

**Files:**
- Modify: `dashboards/src/app/app.config.ts`

- [ ] **Step 1: Set up app config with HttpClient, Router, and APP_INITIALIZER**

Replace `dashboards/src/app/app.config.ts` with:

```typescript
import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { appRoutes } from './app.routes';
import { AuthService } from './core/auth.service';
import { authInterceptor } from './core/auth.interceptor';
import { firstValueFrom } from 'rxjs';

function initAuth(authService: AuthService) {
  return () => firstValueFrom(authService.checkSession());
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(appRoutes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initAuth,
      deps: [AuthService],
      multi: true,
    },
  ],
};
```

The `APP_INITIALIZER` ensures `GET /api/me` is called before the app renders. If the user has a valid session cookie, their context is loaded. If not, auth guard will redirect on first navigation.

- [ ] **Step 2: Commit**

```bash
git add dashboards/src/app/app.config.ts
git commit -m "feat: configure app providers with auth initializer"
```

---

### Task 5: Login Redirect Component

**Files:**
- Create: `dashboards/src/app/features/login/login-redirect.component.ts`

- [ ] **Step 1: Create the login redirect component**

Create: `dashboards/src/app/features/login/login-redirect.component.ts`

```typescript
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login-redirect',
  standalone: true,
  template: `
    <div class="login-container">
      <div class="login-card">
        <h2>Redirecting to login...</h2>
        <p>You will be redirected to the authentication provider.</p>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: #f3f4f6;
    }
    .login-card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      text-align: center;
    }
    h2 { color: #1f2937; margin-bottom: 0.5rem; }
    p { color: #6b7280; }
  `]
})
export class LoginRedirectComponent implements OnInit {
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);

  ngOnInit() {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
    this.authService.login(returnUrl);
  }
}
```

This component exists for the `/login` route. It immediately triggers the server-side login redirect. There is no username/password form because auth is fully handled by PingFederate via the backend.

- [ ] **Step 2: Commit**

```bash
git add dashboards/src/app/features/login/
git commit -m "feat: add login redirect component for OIDC flow"
```

---

### Task 6: Home Component and Layout

**Files:**
- Create: `dashboards/src/app/shared/layout/layout.component.ts`
- Create: `dashboards/src/app/features/home/home.component.ts`

- [ ] **Step 1: Create the layout component**

Create: `dashboards/src/app/shared/layout/layout.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="layout">
      <header class="header">
        <span class="title">Panel</span>
        <div class="user-info">
          <span class="username">{{ authService.user()?.displayName }}</span>
          <button class="logout-btn" (click)="logout()">Logout</button>
        </div>
      </header>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .layout {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    .header {
      background: #1f2937;
      color: white;
      padding: 0.75rem 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .title {
      font-size: 1.25rem;
      font-weight: 600;
    }
    .user-info {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .username {
      color: #d1d5db;
    }
    .logout-btn {
      padding: 0.4rem 0.8rem;
      background: #ef4444;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.875rem;
    }
    .logout-btn:hover {
      background: #dc2626;
    }
    .content {
      flex: 1;
      padding: 1.5rem;
      background: #f3f4f6;
    }
  `]
})
export class LayoutComponent {
  protected authService = inject(AuthService);

  logout() {
    this.authService.logout();
  }
}
```

- [ ] **Step 2: Create the home component**

Create: `dashboards/src/app/features/home/home.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <div class="home">
      <h1>Dashboard</h1>
      <p>Welcome, {{ authService.user()?.displayName }}.</p>

      <div class="context-card">
        <h3>Your Session</h3>
        <dl>
          <dt>Username</dt>
          <dd>{{ authService.user()?.username }}</dd>
          <dt>Email</dt>
          <dd>{{ authService.user()?.email }}</dd>
          <dt>Groups</dt>
          <dd>{{ authService.user()?.groups?.join(', ') || 'None' }}</dd>
          <dt>Permissions</dt>
          <dd>{{ authService.user()?.permissions?.join(', ') || 'None' }}</dd>
        </dl>
      </div>
    </div>
  `,
  styles: [`
    .home h1 {
      margin: 0 0 0.5rem;
      color: #1f2937;
    }
    .home p {
      color: #6b7280;
      margin-bottom: 2rem;
    }
    .context-card {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      max-width: 500px;
    }
    .context-card h3 {
      margin: 0 0 1rem;
      color: #374151;
    }
    dl {
      display: grid;
      grid-template-columns: 120px 1fr;
      gap: 0.5rem;
      margin: 0;
    }
    dt {
      color: #6b7280;
      font-weight: 500;
    }
    dd {
      margin: 0;
      color: #1f2937;
    }
  `]
})
export class HomeComponent {
  protected authService = inject(AuthService);
}
```

- [ ] **Step 3: Commit**

```bash
git add dashboards/src/app/shared/ dashboards/src/app/features/home/
git commit -m "feat: add layout shell and home component"
```

---

### Task 7: Routes Configuration

**Files:**
- Modify: `dashboards/src/app/app.routes.ts`

- [ ] **Step 1: Define application routes**

Replace `dashboards/src/app/app.routes.ts` with:

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LayoutComponent } from './shared/layout/layout.component';

export const appRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login-redirect.component').then(m => m.LoginRedirectComponent),
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/home/home.component').then(m => m.HomeComponent),
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
```

Routes under the layout wrapper require authentication. The `/login` route is outside the guard because it triggers the server-side redirect.

- [ ] **Step 2: Commit**

```bash
git add dashboards/src/app/app.routes.ts
git commit -m "feat: configure routes with auth guard and layout"
```

---

### Task 8: Root Component

**Files:**
- Modify: `dashboards/src/app/app.ts` (or `app.component.ts` depending on CLI output)

- [ ] **Step 1: Simplify root component**

Replace the root component with:

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent {}
```

- [ ] **Step 2: Verify the full flow**

Run: `cd dashboards && ng serve`

1. Open http://localhost:4200 - should trigger auth guard -> redirect to `/api/auth/login?returnUrl=/`
2. Since the backend isn't running locally without PingFederate, the redirect will fail, which is expected.
3. What matters: verify the Angular app compiles, the `APP_INITIALIZER` calls `/api/me`, and the guard triggers the redirect.

- [ ] **Step 3: Commit**

```bash
git add dashboards/src/app/
git commit -m "feat: wire root component and verify auth flow"
```

---

### Task 9: End-to-End Verification

**Files:** (No new files)

- [ ] **Step 1: Run the Angular build to verify no compilation errors**

```bash
cd /d/projects/panel/dashboards && npx ng build
```

Expected: Build succeeds with no errors.

- [ ] **Step 2: Verify proxy config with backend (if backend is running)**

Start the backend:
```bash
cd /d/projects/panel && ./gradlew bootRun --args='--spring.profiles.active=local'
```

Start the frontend:
```bash
cd /d/projects/panel/dashboards && npx ng serve --proxy-config proxy.conf.json
```

Open http://localhost:4200:
- The `APP_INITIALIZER` calls `/api/me` through the proxy -> backend returns 401
- Auth guard triggers -> browser redirects to `/api/auth/login?returnUrl=/`
- Backend redirects to PingFederate (will fail without real PingFederate, that's OK)

- [ ] **Step 3: Final commit**

```bash
git add -A dashboards/
git commit -m "chore: verify frontend build and proxy configuration"
```

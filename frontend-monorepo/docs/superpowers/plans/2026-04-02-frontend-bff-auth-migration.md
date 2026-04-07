# Frontend BFF Auth Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the Angular frontend from SPA-managed OAuth to a BFF-driven session model that loads user state from `/api/me` and redirects login/logout to backend auth endpoints.

**Architecture:** Keep frontend RBAC centered on `AuthorizationService`, but replace OAuth and token management with a thin `AuthService` that only hydrates session state from the backend. Guards, shell UI, and route protection remain, while `/auth/callback`, PKCE, token storage, and bearer injection are removed.

**Tech Stack:** Angular 21, Angular Router, Angular HttpClient, Karma/Jasmine, Spring Security-compatible BFF endpoints

---

### Task 1: Replace SPA OAuth with a thin BFF auth facade

**Files:**
- Modify: `projects/multi-module-app/src/app/core/models/auth.models.ts`
- Modify: `projects/multi-module-app/src/app/core/services/auth.service.ts`
- Modify: `projects/multi-module-app/src/app/core/services/auth.service.spec.ts`

- [ ] **Step 1: Write the failing tests for `/api/me` bootstrap and BFF redirects**

```typescript
it('loads the current user from /api/me during initialize', async () => {
  const initializePromise = service.initialize();
  await flushAsyncWork();

  const userRequest = httpTestingController.expectOne('/api/me');
  expect(userRequest.request.method).toBe('GET');
  userRequest.flush({
    id: 'backend-user',
    username: 'alex',
    displayName: 'Alex Backend',
    permissions: ['module:xms:read']
  });

  await expectAsync(initializePromise).toBeResolved();
  expect(service.currentUserValue?.displayName).toBe('Alex Backend');
});

it('redirects login to the backend auth entrypoint', async () => {
  await service.login('/workspace');

  expect(assignSpy).toHaveBeenCalledWith('/api/auth/login?returnUrl=%2Fworkspace');
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- --watch=false --browsers=ChromeHeadless --include projects/multi-module-app/src/app/core/services/auth.service.spec.ts`
Expected: FAIL because `AuthService` still performs PKCE, callback exchange, and local token/session logic instead of calling `/api/me` and redirecting to `/api/auth/login`.

- [ ] **Step 3: Write the minimal BFF-oriented auth model and service**

```typescript
export interface AuthUser {
  id: string;
  username: string;
  displayName: string;
  email?: string;
  groups: string[];
  entitlements: string[];
  permissions: string[];
  roles: string[];
  dataScopes: Record<string, unknown>;
  claims: Record<string, unknown>;
}

export interface AuthConfig {
  loginPath?: string;
  logoutPath?: string;
  mePath?: string;
}
```

```typescript
async initialize(): Promise<void> {
  await firstValueFrom(this.authConfigService.loadConfig());

  try {
    const currentUser = await this.loadCurrentUser();
    this.currentUserSubject.next(currentUser);
  } catch (error) {
    if (this.isUnauthorized(error)) {
      this.currentUserSubject.next(null);
      return;
    }

    throw error;
  }
}

async login(returnUrl = '/'): Promise<void> {
  const authConfig = await firstValueFrom(this.authConfigService.loadConfig());
  const loginPath = authConfig.loginPath ?? '/api/auth/login';
  const targetUrl = `${loginPath}?returnUrl=${encodeURIComponent(returnUrl)}`;
  this.redirectBrowser(targetUrl);
}

logout(): void {
  const authConfig = this.authConfigService.getConfig();
  this.currentUserSubject.next(null);
  this.redirectBrowser(authConfig.logoutPath ?? '/api/auth/logout');
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- --watch=false --browsers=ChromeHeadless --include projects/multi-module-app/src/app/core/services/auth.service.spec.ts`
Expected: PASS with `/api/me` bootstrap and backend login redirect behavior.

- [ ] **Step 5: Commit**

```bash
git add projects/multi-module-app/src/app/core/models/auth.models.ts \
  projects/multi-module-app/src/app/core/services/auth.service.ts \
  projects/multi-module-app/src/app/core/services/auth.service.spec.ts
git commit -m "refactor: switch auth service to bff session bootstrap"
```

### Task 2: Remove callback/token routes and bearer injection

**Files:**
- Modify: `projects/multi-module-app/src/app/app-routing.module.ts`
- Modify: `projects/multi-module-app/src/app/app.module.ts`
- Modify: `projects/multi-module-app/src/app/features/auth/login/login.component.ts`
- Modify: `projects/multi-module-app/src/app/core/guards/auth.guard.ts`
- Modify: `projects/multi-module-app/src/app/core/interceptors/api.interceptor.ts`
- Delete: `projects/multi-module-app/src/app/features/auth/auth-callback/auth-callback.component.ts`

- [ ] **Step 1: Write the failing test for the login page and interceptor**

```typescript
it('does not add an Authorization header for api requests', () => {
  interceptor.intercept(new HttpRequest('GET', '/api/me'), handler);
  expect(forwardedRequest.headers.has('Authorization')).toBeFalse();
});

it('redirects unauthenticated users through the login route with returnUrl', () => {
  const result = guard.canActivate(routeSnapshot, routerStateSnapshot);
  expect(result.toString()).toContain('/auth/login');
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- --watch=false --browsers=ChromeHeadless`
Expected: FAIL after adding the new assertions because the interceptor still injects bearer tokens and callback-based auth routes are still part of the app shell.

- [ ] **Step 3: Write the minimal route and interceptor cleanup**

```typescript
const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'auth/login', component: LoginComponent },
  { path: 'access-denied', component: AccessDeniedComponent, canActivate: [AuthGuard] },
  { path: 'explorer', component: DataExplorerComponent, canActivate: [AuthGuard] },
  { path: 'workspace', component: WorkspaceComponent, canActivate: [AuthGuard] },
  { path: 'workspace/:moduleId', component: WorkspaceComponent, canActivate: [AuthGuard, ModuleAccessGuard] },
  { path: '**', redirectTo: '' }
];
```

```typescript
intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
  return next.handle(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        this.authService.clearSession();
        void this.router.navigate(['/auth/login'], {
          queryParams: { returnUrl: this.router.url }
        });
      }

      if (error.status === 403) {
        void this.router.navigate(['/access-denied']);
      }

      return throwError(() => error);
    })
  );
}
```

- [ ] **Step 4: Run targeted tests**

Run: `npm test -- --watch=false --browsers=ChromeHeadless`
Expected: PASS with backend login redirect behavior and no callback component usage.

- [ ] **Step 5: Commit**

```bash
git add projects/multi-module-app/src/app/app-routing.module.ts \
  projects/multi-module-app/src/app/app.module.ts \
  projects/multi-module-app/src/app/features/auth/login/login.component.ts \
  projects/multi-module-app/src/app/core/guards/auth.guard.ts \
  projects/multi-module-app/src/app/core/interceptors/api.interceptor.ts \
  projects/multi-module-app/src/app/features/auth/auth-callback/auth-callback.component.ts
git commit -m "refactor: remove spa oauth callback and bearer injection"
```

### Task 3: Align config, docs, and full verification with BFF mode

**Files:**
- Modify: `projects/multi-module-app/public/auth-config.json`
- Modify: `docker/auth-config.json.template`
- Modify: `docker/entrypoint.sh`
- Modify: `docs/pingfederate-oauth2-authorization.md`
- Modify: `README.md`

- [ ] **Step 1: Write the failing test for BFF config defaults**

```typescript
it('normalizes bff auth endpoints', () => {
  authConfigService.loadConfig().subscribe(config => {
    expect(config).toEqual(
      jasmine.objectContaining({
        mePath: '/api/me',
        loginPath: '/api/auth/login',
        logoutPath: '/api/auth/logout'
      })
    );
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- --watch=false --browsers=ChromeHeadless`
Expected: FAIL after adding the config expectations because the config still describes SPA-managed OAuth endpoints and claims-source flow.

- [ ] **Step 3: Write the minimal config and docs update**

```json
{
  "mePath": "/api/me",
  "loginPath": "/api/auth/login",
  "logoutPath": "/api/auth/logout"
}
```

```sh
export AUTH_ME_PATH="${AUTH_ME_PATH:-/api/me}"
export AUTH_LOGIN_PATH="${AUTH_LOGIN_PATH:-/api/auth/login}"
export AUTH_LOGOUT_PATH="${AUTH_LOGOUT_PATH:-/api/auth/logout}"
```

- [ ] **Step 4: Run full verification**

Run: `npm test -- --watch=false --browsers=ChromeHeadless`
Expected: PASS with the updated BFF auth flow.

Run: `npm run build`
Expected: PASS and emit `dist/multi-module-app`.

- [ ] **Step 5: Commit**

```bash
git add projects/multi-module-app/public/auth-config.json \
  docker/auth-config.json.template \
  docker/entrypoint.sh \
  docs/pingfederate-oauth2-authorization.md \
  README.md
git commit -m "docs: align frontend auth config with bff flow"
```

# Frontend BFF Auth Migration Design

## Goal

Move the Angular frontend from SPA-managed OAuth to a BFF-driven auth model so the frontend no longer performs PKCE, code exchange, token storage, or callback handling. The frontend should only consume backend session state and backend-derived RBAC.

## Current State

- The frontend owns the OAuth Authorization Code flow.
- `AuthService` generates PKCE state, exchanges the code for tokens, stores session data in browser storage, and injects bearer tokens.
- Module visibility is already evaluated through `AuthorizationService`, and can keep that boundary.

## Target State

- The backend owns OAuth login and callback handling.
- Frontend login redirects to `/api/auth/login?returnUrl=<path>`.
- Frontend logout redirects to `/api/auth/logout` or falls back to `/logout`.
- Frontend bootstraps auth state only by calling `/api/me`.
- `AuthorizationService` continues to evaluate module access from backend-provided permissions.
- API calls rely on same-site session cookies instead of frontend-managed bearer tokens.

## Recommended Frontend Shape

### 1. Slim Auth Service

Replace the current OAuth-heavy `AuthService` with a thin session facade:

- `initialize()`: call `/api/me`; if it succeeds, store the returned user context; if it returns `401`, clear auth state.
- `login(returnUrl?)`: redirect browser to `/api/auth/login?returnUrl=<encoded path>`.
- `logout()`: clear local user state, then redirect browser to `/api/auth/logout`.
- `isAuthenticated()`: derived from whether `/api/me` returned a current user.
- `currentUser$` / `currentUserValue`: still exposed for UI and guards.

### 2. Remove Frontend OAuth Responsibilities

Delete these responsibilities from the frontend:

- PKCE generation and storage
- OAuth callback parsing
- code exchange
- discovery document fetching
- token persistence
- refresh token handling
- bearer token injection

### 3. Route Simplification

Remove frontend routes and components that exist only for SPA-managed OAuth:

- `/auth/callback`
- any callback-only component logic

Keep a lightweight `/auth/login` page only if the product still wants a visible sign-in screen. Otherwise, guards can redirect straight to `/api/auth/login`.

### 4. API Contract

Frontend depends on these backend endpoints:

- `GET /api/me`
  - `200`: current authenticated user and permissions
  - `401`: unauthenticated
- `GET /api/auth/login?returnUrl=<path>`
  - starts backend-managed login flow
- `POST /api/auth/logout` or `GET /api/auth/logout`
  - clears backend session and redirects as needed

Expected `/api/me` payload:

```json
{
  "id": "u123",
  "username": "alice",
  "displayName": "Alice",
  "email": "alice@example.com",
  "groups": ["xms-users"],
  "entitlements": ["module:xms:read"],
  "permissions": ["module:xms:read", "module:xms:export"],
  "dataScopes": {
    "xms": ["book-a", "book-b"]
  }
}
```

## File-Level Impact

- Simplify `projects/multi-module-app/src/app/core/services/auth.service.ts`
- Remove or repurpose `projects/multi-module-app/src/app/features/auth/auth-callback/auth-callback.component.ts`
- Simplify `projects/multi-module-app/src/app/features/auth/login/login.component.ts`
- Remove token handling from `projects/multi-module-app/src/app/core/interceptors/api.interceptor.ts`
- Remove OAuth bootstrap config that is only needed for SPA-managed auth
- Keep `AuthorizationService`, guards, home filtering, and workspace filtering

## Error Handling

- `401` from `/api/me`: treat as signed out, redirect to login entry when guarded routes are hit.
- `403` from module APIs: continue routing to access denied.
- login redirect failure: show a simple error on the login screen if the frontend keeps that page.

## Testing

- Update `AuthService` tests to cover `/api/me` bootstrap and sign-out behavior.
- Remove tests that assert PKCE or callback behavior.
- Keep authorization tests around permission-based module access.
- Run full frontend tests and build after migration.

## Migration Notes

- During migration, the backend thread should expose `/api/me` before the frontend removes callback/token logic.
- Frontend should prefer backend `permissions` as the RBAC source of truth.
- Frontend should not parse or store access tokens after this migration.

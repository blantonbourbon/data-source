# Frontend BFF Authorization

This frontend now uses a backend-for-frontend session model. The browser never performs the OAuth code exchange, never stores access tokens, and never processes an `/auth/callback` route. Instead, the backend owns the authentication handshake and the frontend only consumes the authenticated user context it exposes.

## Runtime configuration

Edit `projects/multi-module-app/public/auth-config.json` before deploying:

- `mePath`: backend endpoint that returns the authenticated user context, usually `/api/me`
- `loginPath`: backend login entrypoint, usually `/api/auth/login`
- `logoutPath`: backend logout entrypoint, usually `/api/auth/logout`
- `defaultReturnUrl`: route to use when no explicit `returnUrl` is supplied

## Module authorization

Internal modules are configured in `projects/multi-module-app/src/app/core/config/data-query.config.ts`.

External applications are configured in `projects/multi-module-app/public/external-apps.json`.

Each module can declare:

```json
{
  "authorization": {
    "permissions": ["module:xms:read"],
    "match": "all"
  }
}
```

Rules:

- `permissions` is the only frontend RBAC field and should come from the backend user context response
- `match: "all"` requires every listed permission
- `match: "any"` allows any matching permission
- Missing `authorization` means any authenticated user can access the module

At startup the frontend will:

1. load `/auth-config.json`
2. call the backend user context endpoint
3. store the returned user profile in memory
4. evaluate module access from `permissions`

## Enforced entry points

- `/auth/login`: redirects the browser to the backend sign-in entrypoint
- `/`: shows only authorized modules
- `/workspace`: restores only authorized tabs
- `/workspace/:moduleId`: blocked by route guard when unauthorized
- `/access-denied`: explicit 403 page for authenticated but unauthorized users

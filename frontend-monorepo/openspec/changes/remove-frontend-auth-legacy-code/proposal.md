## Why

The frontend authentication flow was recently moved to a backend-for-frontend session model, but the codebase still contains legacy assumptions from the older browser-managed OAuth implementation. Those leftovers increase maintenance cost, leave misleading documentation behind, and keep legacy authorization paths available longer than necessary.

## What Changes

- Remove frontend runtime support for legacy `groups`, `entitlements`, and derived `roles` auth state when evaluating module access.
- Standardize frontend authorization on backend-provided permission codes only.
- Update user-facing auth and access-denied copy so it reflects the current BFF session model instead of PingFederate callback behavior.
- Replace outdated frontend OAuth/PKCE documentation with current BFF-oriented guidance.
- Reject legacy authorization metadata in module configs unless permission codes are present, instead of silently treating it as unrestricted access.

## Capabilities

### New Capabilities
- `frontend-bff-authorization`: Defines the frontend contract for BFF-owned authentication, permission-only RBAC, and current auth documentation.

### Modified Capabilities

## Impact

- Affected frontend auth models and services under `projects/multi-module-app/src/app/core/`
- Affected auth-facing UI copy in home and access-denied screens
- Affected runtime auth docs in `docs/pingfederate-oauth2-authorization.md`
- No backend API expansion; the frontend continues to consume `/api/me`, `/api/auth/login`, and `/api/auth/logout`

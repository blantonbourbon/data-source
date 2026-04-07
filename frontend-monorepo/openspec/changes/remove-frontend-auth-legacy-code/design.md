## Context

The frontend already runs on a backend-for-frontend authentication model: the backend owns the OAuth handshake and the browser only calls `/api/me`, `/api/auth/login`, and `/api/auth/logout`. The remaining problem is that parts of the frontend still preserve structure and language from the previous browser-managed OAuth flow, including legacy authorization fields (`groups`, `entitlements`, `roles`) and outdated PingFederate/PKCE documentation.

This cleanup is cross-cutting because it touches auth state, RBAC evaluation, user-facing copy, and project documentation. The change must stay small and safe because the underlying runtime behavior was already migrated in prior work.

## Goals / Non-Goals

**Goals:**
- Make frontend runtime authorization depend on backend-provided permission codes only
- Remove legacy auth state fields that no longer participate in any supported frontend behavior
- Prevent silent fallback when old-style authorization metadata remains in module config
- Align auth-related UX copy and docs with the current BFF session model

**Non-Goals:**
- Changing backend endpoint contracts or adding new backend APIs
- Reworking module configuration structure beyond removing legacy frontend assumptions
- Renaming the existing documentation file path or fixing the separate trailing-space Git path issue

## Decisions

### 1. Keep the cleanup permission-only at runtime
The frontend will continue to store only the fields it actively uses for authorization. `permissions` remains the single RBAC input for module access checks, while `groups`, `entitlements`, and derived `roles` are removed from runtime user state.

Alternative considered:
- Keep legacy fields for future flexibility. Rejected because they are already unused by configured modules and keep accidental dependency paths alive.

### 2. Treat legacy authorization metadata as invalid, not permissive
If a module authorization object is present but does not contain permission codes, the frontend will reject access instead of treating that config as unrestricted. This is safer than silent pass-through during cleanup.

Alternative considered:
- Ignore unsupported keys and allow access. Rejected because it can hide stale configuration and create accidental exposure.

### 3. Update copy and documentation as part of the same change
The current empty-state and access-denied messages still describe PingFederate groups and entitlements. The runtime guide also still documents frontend PKCE and `/auth/callback`. These references should be updated in the same change so the code and docs stop disagreeing.

Alternative considered:
- Leave docs and copy for a later pass. Rejected because they are directly tied to the same auth cleanup and are already causing confusion.

## Risks / Trade-offs

- [Legacy backend payloads still include `groups` and `entitlements`] → The frontend will ignore them rather than fail, so backend rollout order stays flexible.
- [A stale module config still uses old auth keys only] → The new guard logic will deny access, surfacing the problem instead of silently allowing it.
- [Spec/docs lag behind implementation again] → This change updates the OpenSpec artifacts and the runtime auth guide together.

## Migration Plan

1. Remove legacy auth fields from frontend runtime models and mapping logic.
2. Update authorization checks to use permission codes only and deny unsupported legacy authorization metadata.
3. Refresh auth-facing UI copy and runtime documentation to describe the BFF session model.
4. Verify with focused tests, full frontend tests, and production build.

Rollback is straightforward: restore the previous frontend auth model and authorization service behavior from version control if legacy config compatibility is urgently needed.

## Open Questions

- Whether the old `docs/pingfederate-oauth2-authorization.md` filename itself should be renamed to match the new BFF scope can be handled in a later docs cleanup.

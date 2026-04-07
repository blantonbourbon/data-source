## 1. Permission-Only Auth State

- [x] 1.1 Remove legacy `groups`, `entitlements`, and `roles` fields from frontend auth models and user-context mapping
- [x] 1.2 Update authorization checks so module access is evaluated from permission codes only
- [x] 1.3 Reject legacy authorization metadata without permission codes instead of treating it as unrestricted access

## 2. UX And Documentation Alignment

- [x] 2.1 Update auth-related empty-state and access-denied copy to describe permission-based BFF access
- [x] 2.2 Replace outdated frontend OAuth/PKCE guidance with current BFF auth documentation

## 3. Verification

- [x] 3.1 Add or update focused unit tests covering permission-only auth state and legacy authorization rejection
- [x] 3.2 Run full frontend tests and production build to verify the cleanup

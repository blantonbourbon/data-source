# Improve local frontend dev auth and mock data

## Goal
Make local frontend testing easier by relying on the existing local no-OIDC backend profile and by seeding enough stable mock query data for the frontend modules.

## Requirements
- Keep the local profile usable without interactive OIDC login.
- Do not change non-local security behavior.
- Preserve the existing `/api/me` mock-user behavior in local mode.
- Add richer stable seed data for the queryable local frontend modules so the UI has meaningful results to render.
- Keep the change local-dev focused: no production auth shortcuts and no frontend auth rewrites.

## Acceptance Criteria
- [ ] Running the backend with the local profile does not require interactive OIDC for the frontend flow.
- [ ] `/api/me` still returns a mock authenticated user in local mode.
- [ ] `/api/user/trades` and `/api/user/cryptoassets` return non-empty local data without login.
- [ ] The seeded local data is broad enough to exercise search, filters, tabs, and export behavior in the frontend.
- [ ] Targeted backend tests pass for the local-profile behavior.

## Technical Notes
- Expected files:
  - `panel/src/main/resources/data.sql`
  - `panel/src/test/java/com/data/service/core/security/LocalProfileStartupTest.java`
  - optional local-dev docs if needed
- Current repo reality:
  - `panel/src/main/resources/application-local.properties` already sets `panel.security.local-dev.auth-disabled=true`
  - `CurrentUserController` already returns the configured local-dev mock user from `/api/me`
  - `AuthController` already skips the OIDC redirect when local auth is disabled
- Assumption:
  - “disable the oidc or use a mocked oidc” is already satisfied by the backend local profile, so this task should reinforce and verify that behavior rather than invent a second local auth mechanism.

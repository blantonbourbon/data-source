## ADDED Requirements

### Requirement: Frontend auth state uses the BFF user context contract
The frontend SHALL derive authenticated user state from the backend user context endpoint and SHALL keep only the fields needed for the BFF session model and permission-based authorization.

#### Scenario: Successful user context load
- **WHEN** the application initializes and `/api/me` returns an authenticated user payload with permission codes
- **THEN** the frontend stores the user identity, permission codes, data scopes, and claims in memory
- **AND** the frontend does not require frontend-managed OAuth tokens or callback processing

#### Scenario: Unauthorized user context load
- **WHEN** the application initializes and `/api/me` returns `401 Unauthorized`
- **THEN** the frontend clears its current user state
- **AND** the user is treated as unauthenticated until login restarts through the backend entrypoint

### Requirement: Frontend module authorization is permission-only
The frontend SHALL evaluate module access using configured permission codes only.

#### Scenario: Module access with matching permissions
- **WHEN** a module requires permission codes that are all present in the current user permissions
- **THEN** the frontend allows the module to appear in navigation and route access checks

#### Scenario: Legacy authorization metadata without permissions
- **WHEN** a module authorization object contains only unsupported legacy metadata such as groups or entitlements
- **THEN** the frontend denies access for that module
- **AND** the module is not treated as unrestricted access

### Requirement: Auth messaging reflects the BFF model
Frontend auth-related guidance SHALL describe the backend-owned sign-in flow and permission-based access checks.

#### Scenario: Empty authorized-modules state
- **WHEN** an authenticated user has no modules available after permission filtering
- **THEN** the frontend explains that the current permission set does not map to any configured module
- **AND** it does not instruct the user to troubleshoot PingFederate group or entitlement mapping in the browser

#### Scenario: Runtime auth documentation
- **WHEN** a developer reads the frontend auth guidance document
- **THEN** the document describes `/api/me`, `/api/auth/login`, and `/api/auth/logout` as the frontend auth contract
- **AND** it does not describe frontend PKCE token exchange or `/auth/callback` handling

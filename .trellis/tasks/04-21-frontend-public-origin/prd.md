# Refactor frontend runtime origin config

## Goal
Allow the frontend container runtime to accept `PUBLIC_ORIGIN` without a URL scheme so the same value can be reused in ingress-related configuration, and remove redundant auth runtime configuration from the frontend entrypoint.

## Requirements
- Treat `PUBLIC_ORIGIN` as a public host with optional port, not a full URL.
- Keep `BACKEND_ORIGIN` as a full backend URL.
- Continue forwarding `X-Forwarded-Host`, `X-Forwarded-Port`, and `X-Forwarded-Proto` to the backend for Spring Security callback resolution.
- Fail fast with a clear error when `PUBLIC_ORIGIN` includes a scheme or invalid nginx-breaking characters.
- Stop requiring auth-related environment variables in the frontend container entrypoint.
- Stop generating `/tmp/auth-config.json` in the frontend container; serve the built static `public/auth-config.json` instead.
- Document the new runtime environment contract in frontend deployment docs.

## Acceptance Criteria
- [ ] `frontend-monorepo/docker/entrypoint.sh` accepts `PUBLIC_ORIGIN=app.example.com` and `PUBLIC_ORIGIN=localhost:8080`.
- [ ] Generated nginx proxy headers still use the public host, port, and proto expected by the backend.
- [ ] The frontend container no longer requires auth-related env vars at startup.
- [ ] `/auth-config.json` is served from the built frontend assets instead of a `/tmp` generated file.
- [ ] README examples and variable descriptions match the new contract.
- [ ] Shell syntax validation passes for the updated script.

## Technical Notes
- Target files to update:
  - `frontend-monorepo/docker/entrypoint.sh`
  - `frontend-monorepo/README.md`
- Contract definition:
  - `PUBLIC_ORIGIN`: `host` or `host:port`, no `http://` or `https://`
  - `PUBLIC_SCHEME`: optional, `http` or `https`, defaults to `https`
  - `BACKEND_ORIGIN`: full backend origin with scheme
  - auth config source: built `projects/multi-module-app/public/auth-config.json`
- Validation and error matrix:
  - Good: `PUBLIC_ORIGIN=app.example.com`, `PUBLIC_SCHEME=https`
  - Base: `PUBLIC_ORIGIN=localhost:8080`, `PUBLIC_SCHEME=http`
  - Bad: `PUBLIC_ORIGIN=https://app.example.com`
  - Bad: `PUBLIC_ORIGIN=app.example.com/path`

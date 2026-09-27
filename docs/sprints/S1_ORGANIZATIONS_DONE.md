# Sprint S1 — Auth hardening + Organizations (Done)

## Delivered
- PRD-01 AC verified (mock auth, `/me`, org isolation, permissions)
- PRD-02 Organization APIs:
  - `POST /api/v1/organizations`
  - `GET /api/v1/organizations`
  - `GET /api/v1/organizations/{id}`
  - `POST /api/v1/organizations/{id}/users`
  - `GET /api/v1/organizations/{id}/users`
- Unique slug, ACTIVE status, creator auto-membership
- Duplicate membership → 409 `MEMBERSHIP_EXISTS`
- Cross-org GET → 403
- Audit events: `ORGANIZATION_CREATED`, `ORGANIZATION_USER_ADDED`
- Unit + API tests (UT-02-*)
- Frontend Organizations page

## Try it
1. Restart backend if running: `.\mvnw.cmd spring-boot:run`
2. Swagger: create org, then set `X-Org-Id` to returned id for get/add-user
3. UI: http://localhost:5173/organizations

## Next
**Sprint S2** — PRD-03 Source material upload + immutable versions

# PRD-02 — Organization Management

| Field | Value |
|--------|--------|
| Status | Done (S1) |
| Priority | P0 |
| Module | `common` / org |

## 1. Problem
All source and course data is tenant-scoped.

## 2. Goals
- Create/list organizations (bootstrap).
- Attach users to orgs with a role.
- Enforce `organization_id` on owned entities.

## 3. Non-Goals
Billing, multi-org user switching UI polish, SSO org provisioning.

## 4. Data
```
organizations(id, name, status, created_at)
organization_users(id, organization_id, user_id, role, created_at)
users(id, display_name, email, created_at)  -- seed/mock users OK
```

## 5. API
- `POST /api/v1/organizations`
- `GET /api/v1/organizations/{id}`
- `POST /api/v1/organizations/{id}/users`

## 6. Acceptance Criteria
- [x] Org created with unique name per system (or slug)  
- [x] User-org-role membership persisted  
- [x] Queries never return other org’s entities  

## 7. Unit Test Cases
See UNIT_TEST_CASES §PRD-02.

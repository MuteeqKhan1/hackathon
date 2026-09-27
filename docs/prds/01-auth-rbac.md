# PRD-01 — Auth & RBAC (Mock)

| Field | Value |
|--------|--------|
| Status | Done (S0/S1) |
| Priority | P0 |
| Module | `common` / security |

## 1. Problem
Prototype needs enforceable roles without full SSO.

## 2. Goals
- Identify user, org, role on every request.
- Deny cross-org access.
- Gate permissions listed in REQ-001.

## 3. Non-Goals
OIDC, password reset, MFA, refresh-token rotation.

## 4. Design
Incoming headers (dev) or signed mock JWT:
- `X-User-Id`
- `X-Org-Id`
- `X-Role` ∈ {CONTENT_OWNER, INSTRUCTOR, STUDENT}

`SecurityContext` holds `AuthenticatedUser`. Services call `PermissionGuard.require(permission)`.

## 5. Permissions
SOURCE_CREATE, SOURCE_UPDATE, SOURCE_VIEW, SOURCE_PUBLISH, SOURCE_VERSION_VIEW, IMPACT_VIEW, COURSE_CREATE, COURSE_EDIT, COURSE_VIEW, COURSE_PUBLISH, CONTENT_GENERATE, CONTENT_EDIT, SYNC_REVIEW, LESSON_VIEW, QUIZ_ATTEMPT, ASSIGNMENT_SUBMIT, PROGRESS_VIEW

## 6. API
No login API in v1. Optional `GET /api/v1/me` returns current principal.

## 7. Acceptance Criteria
- [x] Missing headers → 401  
- [x] Wrong org resource → 403  
- [x] Role without permission → 403  
- [x] `/me` returns userId, orgId, role, permissions  

## 8. Unit Test Cases
See `docs/testing/UNIT_TEST_CASES.md` §PRD-01.

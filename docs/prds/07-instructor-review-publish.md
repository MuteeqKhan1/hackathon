# PRD-07 — Instructor Review & Course Publishing

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P0 |
| Module | `content` + `course` |

## 1. Problem
AI output must not go live without human control.

## 2. Goals
- Edit asset content (sets `manual_modification=true`).
- Approve / Reject asset versions.
- Publish course when policy satisfied.
- Preserve full version history.

## 3. State Machine (asset version)
```
GENERATED → PENDING_REVIEW → APPROVED → (course publish uses this)
                       ↘ REJECTED → (regenerate → new version)
```

## 4. Course publish rules
- Default: all EXPLANATION + QUIZ assets for topics marked required must be APPROVED.
- Instructor may publish with explicit `allowIncomplete=true` for optional assets only (flag audited).

## 5. API
- `PATCH /api/v1/content-assets/{id}` (edit body)
- `POST /api/v1/content-assets/{id}/approve`
- `POST /api/v1/content-assets/{id}/reject`
- `POST /api/v1/courses/{id}/publish`

## 6. Acceptance Criteria
- [ ] Edit flips manual_modification  
- [ ] Approve records approved_by/at  
- [ ] Publish blocked when required PENDING_REVIEW (unless explicit override policy)  
- [ ] Audit: CONTENT_EDITED, CONTENT_APPROVED, COURSE_PUBLISHED  

## 7. Unit Test Cases
See UNIT_TEST_CASES §PRD-07.

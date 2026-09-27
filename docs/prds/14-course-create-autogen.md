# PRD-14 — Auto-Generate Drafts on Course Create

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P1 |
| Module | `course` + `content` |
| Sprint | S13 |

## 1. Problem
Instructors expect that selecting source material at course create starts draft structure and content generation, not only an empty DRAFT shell.

## 2. Goals
- Optional flag `generateDrafts=true` on course create (default true in UI).
- After create: enqueue structure generation, then per-topic EXPLANATION+QUIZ+VIDEO (async).
- All outputs remain PENDING_REVIEW — never auto-publish.

## 3. Non-Goals
Skipping instructor review; blocking HTTP until all AI finishes (must stay 202/async).

## 4. Domain Rules
- Course create still binds published source version.
- Pipeline: CREATE → STRUCTURE_GENERATE op → for each topic CONTENT_GENERATE.
- Failures mark operations FAILED; course remains DRAFT.
- Student visibility unchanged until publish.

## 5. API
- `POST /api/v1/courses` body adds `generateDrafts?: boolean`
- Response includes `bootstrapOperationId` when kicked off

## 6. Acceptance Criteria
- [ ] `generateDrafts=false` creates empty DRAFT only  
- [ ] `generateDrafts=true` starts async structure op  
- [ ] No course reaches PUBLISHED without instructor publish  

## 7. Unit Test Cases
See UNIT_TEST_CASES §PRD-14.

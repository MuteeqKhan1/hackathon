# Sprint S6 — Instructor Review & Publish (Done)

## Delivered (PRD-07)
- `ContentReviewService`: edit (manual_modification), approve, reject
- `CoursePublishService`: requires EXPLANATION+QUIZ APPROVED per topic; `allowIncomplete=true` override with audit
- APIs:
  - `PATCH /api/v1/content-assets/{id}`
  - `POST /api/v1/content-assets/{id}/approve`
  - `POST /api/v1/content-assets/{id}/reject`
  - `POST /api/v1/courses/{id}/publish` body `{ allowIncomplete?: boolean }`
- UI: Save edit / Approve / Reject / Publish course
- Audit: `CONTENT_EDITED`, `CONTENT_APPROVED`, `CONTENT_REJECTED`, `COURSE_PUBLISHED`, `COURSE_PUBLISH_INCOMPLETE_ALLOWED`
- Unit tests UT-07-01…06

## Acceptance criteria
- [x] Edit flips manual_modification  
- [x] Approve records approved_by/at  
- [x] Publish blocked when required PENDING_REVIEW (unless allowIncomplete)  
- [x] Audit events recorded  

## Try it
1. Restart backend if needed
2. INSTRUCTOR + org `eabfcdf7…`
3. Open EXPLANATION → **Approve**; open QUIZ → **Approve**
4. **Publish course** → status becomes `PUBLISHED`

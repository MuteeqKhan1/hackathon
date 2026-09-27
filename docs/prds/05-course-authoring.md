# PRD-05 — Course Authoring & Structure

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P0 |
| Module | `course` |

## 1. Problem
Instructors need courses bound to a specific source version with editable AI-proposed structure.

## 2. Goals
- Create course: source material, source version, language, audience, level, duration.
- Async AI structure generation → chapters/topics.
- Instructor add/delete/rename/reorder chapters and topics.
- Track `current_source_version_id`; set UPDATE_REQUIRED when sync finds impact.

## 3. Non-Goals
Real-time multiplayer editing; importing SCORM.

## 4. Data
```
courses(id, organization_id, title, description, source_material_id, current_source_version_id,
  language, audience, level, duration_hours, status, created_by, created_at, updated_at)
course_chapters(id, course_id, title, sequence, status)
course_topics(id, chapter_id, title, sequence, status)
```

## 5. API
- `POST /api/v1/courses`
- `GET /api/v1/courses/{id}`
- `PATCH /api/v1/courses/{id}`
- `POST /api/v1/courses/{id}/generate-structure` → 202
- `GET/PATCH` chapters & topics endpoints as needed for reorder

## 6. Acceptance Criteria
- [ ] Course cannot bind to another org’s source  
- [ ] Structure generation creates sequenced chapters/topics  
- [ ] Reorder persists stable ids (no delete/recreate required)  
- [ ] Status transitions validated  

## 7. Unit Test Cases
See UNIT_TEST_CASES §PRD-05.

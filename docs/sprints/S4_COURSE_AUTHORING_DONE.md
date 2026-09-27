# Sprint S4 — Course Authoring (Done)

## Delivered (PRD-05)
- Flyway `V4__courses.sql`: `courses`, `course_chapters`, `course_topics`
- Domain: `Course` / `CourseChapter` / `CourseTopic` + statuses
- `CourseService.create` — same-org source only; binds **PUBLISHED** version; status `DRAFT`
- `StructureApplier` + `SectionBasedStructureProposer` (sections → chapter/topics; LLM deferred to S5)
- Async `POST .../generate-structure` → 202 + `COURSE_STRUCTURE_GENERATE` operation
- Topic/chapter reorder preserving ids
- `CoursePublishService` blocks DRAFT→PUBLISHED (`APPROVAL_REQUIRED` / PRD-07)
- `markUpdateRequired` → `UPDATE_REQUIRED`
- APIs: create/list/get/patch/generate-structure/reorder/publish-gate
- UI: Course Authoring page (create, generate, ↑↓ reorder)
- Unit tests: UT-05-01…06 coverage

## Acceptance criteria
- [x] Course cannot bind to another org’s source  
- [x] Structure generation creates sequenced chapters/topics  
- [x] Reorder persists stable ids  
- [x] Status transitions validated (publish blocked; UPDATE_REQUIRED)

## Try it
1. Restart backend (Flyway V4): `.\mvnw.cmd spring-boot:run`
2. Role **INSTRUCTOR**, org `eabfcdf7…`
3. Course Authoring → create course on Physics Grade 10 **published** version → Generate structure → reorder topics

## Note
Course create/edit requires **INSTRUCTOR** (not CONTENT_OWNER). Source upload still needs CONTENT_OWNER.

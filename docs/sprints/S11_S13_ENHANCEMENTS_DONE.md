# Sprint S11–S13 — Enhancements (Done)

## PRDs added
- PRD-11 Video storyboard generation
- PRD-12 Multilingual preferences
- PRD-13 Learning pace (EASY/MEDIUM/HARD)
- PRD-14 Auto structure gen on course create (`generateDrafts`)

## Implemented
- `AssetType.VIDEO` + schema + heuristic scenes + `VIDEO_GENERATION` prompt (Flyway V8)
- Generate content includes VIDEO; student scene player
- `student_preferences` + GET/PUT `/api/v1/student/preferences`
- Course create `generateDrafts=true` → structure bootstrap op
- Unit tests: VideoSchema, preferences resolve, CourseBootstrap

## Try it
1. Restart backend (V8 migration)
2. Course Authoring → Generate content → approve **VIDEO** too
3. Publish → Student Learning → play scenes; set language/pace prefs
4. Create course → structure auto-starts when `generateDrafts` true

# PRD-11 — Video Lesson Generation

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P0 |
| Module | `content` + `ai` |
| Sprint | S11 |

## 1. Problem
Instructors want a video-style lesson per topic grounded in source material, not only text explanation + quiz.

## 2. Goals
- Generate `VIDEO` content assets as **scene storyboards** (narration + on-screen text + duration) from RAG chunks.
- Same lineage, review, sync, and publish rules as EXPLANATION/QUIZ.
- Students play scenes sequentially (prototype player). Never auto-publish.

## 3. Non-Goals
Photoreal MP4 rendering, avatar/TTS pipeline, YouTube upload (Phase 2 behind `VideoRenderer`).

## 4. Domain Rules
- `asset_type = VIDEO`; status GENERATED → PENDING_REVIEW → APPROVED.
- Schema: `title`, `scenes[{sequence, narration, onScreenText, durationSeconds, citedSectionIds[]}]`, `citedSectionIds[]`.
- Write `content_source_mapping` at generation.
- Sync REGENERATE may target VIDEO like other assets.
- Publish: VIDEO is **optional** for course publish (EXPLANATION+QUIZ remain required); if present must be APPROVED when `requireVideo=true` later.

## 5. Data
Reuse `content_assets` / `content_asset_versions`. Prompt key `VIDEO_GENERATION`.

## 6. API
- Existing generate: `{ assetTypes: ["EXPLANATION","QUIZ","VIDEO"] }`
- Student topic payload includes `video` storyboard when approved.

## 7. Acceptance Criteria
- [ ] Valid VIDEO JSON persists with mappings  
- [ ] Invalid schema rejected  
- [ ] Student can play scenes for APPROVED video  
- [ ] Instructor must approve before students see video  

## 8. Unit Test Cases
See UNIT_TEST_CASES §PRD-11.

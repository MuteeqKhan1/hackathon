# PRD-06 — AI Content Generation (Explanation + Quiz)

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P0 |
| Module | `content` + `assessment` + `ai` |

## 1. Problem
Instructors need grounded explanations and quizzes without inventing unsupported facts.

## 2. Goals
- Generate EXPLANATION and QUIZ for a topic using RAG over source sections.
- Validate structured JSON schema before persist.
- Write `content_source_mapping` for every used section.
- Record promptVersion, modelName, modelVersion, generationMethod=AI.
- Asset status starts GENERATED / PENDING_REVIEW.

## 3. Non-Goals
PPT, video, assignment, practice packs; streaming token UI (optional later).

## 4. Flow
```
Topic → retrieve top-k chunks (filter by course source version)
     → GenerationRequest(assetType, language, audience, difficulty, sourceSectionIds)
     → AiProvider.generate
     → schema validate + grounding checks (section ids must be subset of retrieved)
     → save content_asset + version + mappings
```

## 5. Data
```
content_assets(id, course_id, chapter_id, topic_id, asset_type, status, current_version_id, ...)
content_asset_versions(id, content_asset_id, version_number, content_json, language,
  source_version_id, generation_method, prompt_version, model_name, model_version,
  status, manual_modification, sync_policy, created_by, created_at, approved_by, approved_at)
content_source_mapping(id, content_asset_id, source_section_id, source_section_version_id, relationship_type)
prompt_versions(id, prompt_key, version, template, created_at)
quizzes / quiz_questions (normalized from QUIZ asset or linked)
```

## 6. API
- `POST /api/v1/courses/{courseId}/topics/{topicId}/generate` body: `{ assetTypes: ["EXPLANATION","QUIZ"] }` → 202
- `POST /api/v1/content-assets/{id}/regenerate` → 202
- `GET /api/v1/content-assets/{id}`

## 7. Acceptance Criteria
- [ ] Invalid LLM JSON rejected; no partial corrupt version committed  
- [ ] Mapping rows exist after successful generation  
- [ ] Regenerate increments version_number; old version retained  
- [ ] AiProvider is the only LLM dependency  

## 8. Unit Test Cases
See UNIT_TEST_CASES §PRD-06.

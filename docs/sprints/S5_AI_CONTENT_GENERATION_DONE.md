# Sprint S5 — AI Content Generation (Done)

## Delivered (PRD-06)
- Flyway `V5__content_assets.sql`: assets, versions, `content_source_mapping`, prompts, quizzes
- `SchemaValidator` + `GroundingValidator` (reject invalid / ungrounded LLM JSON)
- `PromptRegistry` (seeded EXPLANATION + QUIZ templates)
- `SourceChunkRetriever` (top-k chunks for course source version)
- `GenerationService` + async `CONTENT_GENERATE` / `CONTENT_REGENERATE` operations
- `MappingWriter` — PRIMARY/SUPPORTING lineage rows at generation time
- `HeuristicAiProvider` default (`app.ai.provider=heuristic`); OpenAI switch stub ready
- APIs:
  - `POST /api/v1/courses/{courseId}/topics/{topicId}/generate`
  - `POST /api/v1/content-assets/{id}/regenerate`
  - `GET /api/v1/content-assets/{id}`
  - `GET /api/v1/courses/{courseId}/topics/{topicId}/content-assets`
- UI: **Generate content** per topic + asset preview with mappings
- Unit tests UT-06-01…08 (core coverage)

## Acceptance criteria
- [x] Invalid LLM JSON rejected; no corrupt version  
- [x] Mapping rows after successful generation  
- [x] Regenerate increments version_number; prior versions retained  
- [x] `AiProvider` is the only LLM dependency  

## Try it
1. Restart backend (Flyway V5): `.\mvnw.cmd spring-boot:run`
2. Role **INSTRUCTOR**, org `eabfcdf7…`
3. Course bound to **v5** (has sections) → Generate structure → **Generate content** on a topic
4. Click `[EXPLANATION:PENDING_REVIEW]` / `[QUIZ:…]` to preview JSON + mappings

## AI config
```yaml
app.ai.provider: heuristic   # default local stub
# app.ai.provider: openai    # when real key + adapter implemented
# OPENAI_API_KEY=...
```

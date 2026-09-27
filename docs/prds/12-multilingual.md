# PRD-12 — Multilingual Content

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P1 |
| Module | `content` + `student` |
| Sprint | S12 |

## 1. Problem
Students need to learn in their preferred language while content stays grounded in the same source lineage.

## 2. Goals
- Persist `language` on content asset versions (already present); generate/regenerate for a requested language.
- Student preference (`preferred_language`) selects which approved variant to show.
- Fallback to course default language when preferred variant missing.

## 3. Non-Goals
Realtime machine translation of live video frames; community translation UI.

## 4. Domain Rules
- One logical asset per topic+type; language variants are **versions** or parallel assets keyed by `(topic, type, language)` — v1 enhancement uses **version language field** + generate with `language` param.
- Lineage unchanged across languages (same source sections).
- Sync regen uses course/student target language from request.

## 5. Data
```
student_preferences(student_id, organization_id, preferred_language, preferred_pace, updated_at)
```
`content_asset_versions.language` already exists.

## 6. API
- `GET/PUT /api/v1/student/preferences`
- Generate body may include `language`
- Student topic returns content matching preference with fallback

## 7. Acceptance Criteria
- [ ] Student can set preferred language  
- [ ] Topic lesson returns matching language when approved variant exists  
- [ ] Fallback to course language when missing  

## 8. Unit Test Cases
See UNIT_TEST_CASES §PRD-12.

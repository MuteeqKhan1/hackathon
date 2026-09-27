# PRD-09 — Change Detection & Synchronization Engine

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P0 — differentiating capability |
| Module | `sync` |

## 1. Problem
When source PDF version N+1 is published, instructors must see precise impact and choose what to update.

## 2. Goals
- Create idempotent `sync_event` for (material, oldVersion, newVersion).
- Detect section-level changes and classify type.
- Resolve impacted assets via lineage.
- Score impact LOW/MEDIUM/HIGH with deterministic configurable rules.
- Notify instructors (in-app).
- Support selective REGENERATE / IGNORE / REJECT / ACCEPT.
- Regeneration creates draft versions for review (PRD-06/07).
- Mark course UPDATE_REQUIRED until resolved or ignored per policy.

## 3. Non-Goals
Fully autonomous publish; three-way merge UI (flag conflict only when manual_modification).

## 4. Impact rules (initial matrix)

| Change signal | Explanation | Quiz |
|---------------|-------------|------|
| METADATA_CHANGED / typo heuristic | LOW | LOW |
| example-oriented MODIFIED | MEDIUM | MEDIUM |
| definition/formula/REMOVED/ADDED core | HIGH | HIGH |

Heuristic v1: formula tokens (`=`, symbols), length delta thresholds, section_type — configurable in DB/YAML, not hard-coded forever.

## 5. Data
```
sync_events(id, source_material_id, old_version_id, new_version_id, status, idempotency_key, created_at, completed_at)
impact_analysis(id, sync_event_id, course_id, content_asset_id, impact_level, reason, recommended_action, status)
sync_actions(id, impact_analysis_id, action, requested_by, status, old_asset_version_id, new_asset_version_id, ...)
```

Statuses: DETECTED → ANALYZING → IMPACT_IDENTIFIED → NOTIFIED → IN_PROGRESS → COMPLETED | FAILED

## 6. API
- `GET /api/v1/sync-events`
- `GET /api/v1/sync-events/{id}`
- `GET /api/v1/sync-events/{id}/impact`
- `POST /api/v1/sync-events/{id}/actions` `{ assetId, action }`
- Bulk: `{ actions: [...] }`

## 7. Conflict handling
If `manual_modification=true`, recommended_action = REVIEW_CONFLICT; UI shows source old/new + instructor content; no silent overwrite.

## 8. Acceptance Criteria
- [ ] Duplicate SOURCE_VERSION_PUBLISHED does not duplicate sync_event (idempotency_key)  
- [ ] Unchanged hashes produce zero MODIFIED impacts  
- [ ] Lineage-missing assets are not falsely listed  
- [ ] Selective regen only touches selected assets  
- [ ] Old quiz version still readable after new published  
- [ ] Notification created for course instructors  
- [ ] Audit: SOURCE_CHANGE_DETECTED, IMPACT_IDENTIFIED, SYNC_*, CONTENT_REGENERATED  

## 9. Unit Test Cases
See UNIT_TEST_CASES §PRD-09 (heaviest suite).

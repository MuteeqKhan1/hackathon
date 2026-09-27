# PRD-08 — Content Lineage

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P0 |
| Module | `content` |

## 1. Problem
Sync is impossible without answering: which course assets depend on which source sections?

## 2. Goals
- Persist mappings at generation time.
- Query forward (asset → sections) and reverse (section → assets).
- Support relationship types and sync policy.

## 3. Non-Goals
Automatic re-inference of lineage for manually pasted content without AI (instructor may attach CUSTOM).

## 4. API
- `GET /api/v1/content-assets/{id}/sources`
- `GET /api/v1/source-sections/{id}/dependents`
- `PUT /api/v1/content-assets/{id}/sync-policy` body: `{ policy: "AUTO"|"MANUAL" }`

## 5. Acceptance Criteria
- [ ] Reverse lookup returns all assets mapping to section across courses in org  
- [ ] CUSTOM relationship excluded from auto REGENERATE recommendations (PRD-09)  
- [ ] MANUAL policy suppresses repeat notifications for same section change signature  

## 6. Unit Test Cases
See UNIT_TEST_CASES §PRD-08.

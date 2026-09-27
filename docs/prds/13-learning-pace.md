# PRD-13 — Learning Pace (Easy / Medium / Hard)

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P1 |
| Module | `content` + `student` |
| Sprint | S12 |

## 1. Problem
Students need different depth and quiz difficulty (easy / medium / hard) for the same topic and source.

## 2. Goals
- Support `pace` on generation: EASY | MEDIUM | HARD.
- Store pace on asset version metadata (`content_json.meta.pace` or column).
- Student preference selects pace; generate/show matching variant.

## 3. Non-Goals
Adaptive path engine that auto-changes pace mid-course (Phase 2).

## 4. Domain Rules
- Pace affects explanation length, video scene count, and quiz difficulty wording — still grounded.
- Unique key for variants: `(topic_id, asset_type, language, pace)` via content JSON + query filter in student layer for v1.
- No permanent “slow/fast learner” labels (align PRD-10).

## 5. Data
- `student_preferences.preferred_pace`
- Optional column later: `content_asset_versions.pace` — v1 stores in JSON `pace` field on root.

## 6. API
- Preferences include `preferredPace`
- Generate may include `pace`
- Student topic filters by pace with fallback MEDIUM → course default

## 7. Acceptance Criteria
- [ ] Generate with pace embeds pace in content  
- [ ] Student preference EASY returns EASY content when available  
- [ ] Missing pace falls back to MEDIUM then any approved  

## 8. Unit Test Cases
See UNIT_TEST_CASES §PRD-13.

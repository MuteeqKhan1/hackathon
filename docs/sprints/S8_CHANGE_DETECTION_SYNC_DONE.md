# Sprint S8 — Change Detection & Sync (Done)

## Delivered (PRD-09)
- Flyway `V6__sync_engine.sql` — `sync_events`, `impact_analysis`, `sync_actions`, `notifications`
- Idempotent `SyncEventFactory` + `SyncPipelineService` after source publish (N→N+1)
- `ChangeDetectionService` scoring (typo LOW / formula HIGH / removed HIGH)
- `ImpactAnalyzer` via lineage + `RecommendationFilter`
- `SyncActionService` — REGENERATE (draft) / IGNORE / ACCEPT / REJECT; REVIEW_CONFLICT blocks overwrite
- In-app `NotificationService`; HIGH → course `UPDATE_REQUIRED`
- APIs: `GET/POST /api/v1/sync-events…`
- UI: **Synchronization** page
- Unit tests UT-09-01…13

## Try it
1. Restart backend (Flyway V6)
2. As CONTENT_OWNER: upload + publish a **new** PDF version of Physics Grade 10 (after v2)
3. As INSTRUCTOR: open **Synchronization** → select event → Regenerate / Ignore

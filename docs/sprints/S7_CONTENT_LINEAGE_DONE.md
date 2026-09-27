# Sprint S7 — Content Lineage (Done)

## Delivered (PRD-08)
- `GET /api/v1/content-assets/{id}/sources` — forward lineage
- `GET /api/v1/source-sections/{id}/dependents` — reverse lineage (org-scoped)
- `PUT /api/v1/content-assets/{id}/sync-policy` — `AUTO` | `MANUAL`
- `RecommendationFilter` — excludes CUSTOM; suppresses MANUAL + same change signature
- UI: **Show sources**, **Sync MANUAL/AUTO** on asset preview
- Unit tests UT-08-01…05

## Try it
1. Restart backend if needed
2. Open an approved asset → **Show sources**
3. Optionally set **Sync MANUAL**

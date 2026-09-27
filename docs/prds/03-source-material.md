# PRD-03 — Source Material & Versioning

| Field | Value |
|--------|--------|
| Status | Done (S2) |
| Priority | P0 |
| Module | `source` |

## 1. Problem
Organizations need authoritative PDF materials with immutable history.

## 2. Goals
- CRUD metadata for source materials.
- Upload PDF → object storage → new version.
- Publish version (triggers parse/sync pipeline asynchronously).
- List/compare versions.

## 3. Non-Goals
Non-PDF formats; in-browser PDF editing; collaborative source editing.

## 4. Domain Rules
- Version numbers monotonic per material: 1,2,3…
- Published version content immutable.
- `content_hash` of full file stored on version.
- Status: DRAFT | PROCESSING | PUBLISHED | FAILED

## 5. Data
```
source_materials(id, organization_id, title, description, subject, status, created_by, created_at)
source_material_versions(id, source_material_id, version_number, storage_key, file_content_hash,
  status, created_by, created_at, published_at)
```

## 6. API
- `POST /api/v1/source-materials`
- `POST /api/v1/source-materials/{id}/versions` (multipart PDF)
- `GET /api/v1/source-materials/{id}`
- `GET /api/v1/source-materials/{id}/versions`
- `POST /api/v1/source-materials/{id}/versions/{versionId}/publish` → 202 operation

## 7. Events
On publish success after parse: `SOURCE_VERSION_PUBLISHED { sourceMaterialId, oldVersionId?, newVersionId }`

## 8. Acceptance Criteria
- [x] Non-PDF rejected with clear error code  
- [x] Oversized file rejected  
- [x] Second publish creates version 2; version 1 untouched  
- [x] Publish returns operationId; processing async  
- [x] Audit: SOURCE_UPLOADED, SOURCE_VERSION_CREATED, SOURCE_VERSION_PUBLISHED  

## 9. Unit Test Cases
See UNIT_TEST_CASES §PRD-03.

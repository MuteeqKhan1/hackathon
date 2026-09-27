# Sprint S2 — Source Material & Versioning (Done)

## Delivered
- Flyway `V2__source_materials.sql`
- Create/list/get source materials (org-scoped)
- PDF-only upload → local object storage (`./data/storage`) + SHA-256 hash
- Monotonic versions (v1, v2, …); published versions immutable
- Publish → `202` + `operationId` (async worker marks PUBLISHED)
- `GET /api/v1/operations/{id}`
- Audit: SOURCE_CREATED, SOURCE_UPLOADED, SOURCE_VERSION_CREATED, SOURCE_VERSION_PUBLISHED
- Unit tests UT-03 (validator, versioning, publish)
- Frontend Source Materials page

## Note
Full PDF parse/RAG is **Sprint S3 (PRD-04)**. S2 publish completes version lifecycle without section fingerprinting yet.

## Try it
1. Restart backend (Flyway V2 applies)
2. Role = CONTENT_OWNER, active org = Physics Academy
3. Source Materials → create → upload PDF → Publish → wait for COMPLETED

## Next
**Sprint S3** — PRD-04 PDF parsing, section hashes, change detection prep

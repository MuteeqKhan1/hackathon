# Sprint S3 — PDF Parsing & Section Fingerprints (Done)

## Delivered
- Flyway `V3__source_sections.sql` (sections, section versions, chunks)
- Apache PDFBox parser (outline → headings → paragraphs fallback)
- `TextNormalizer`, `ContentHasher`, `StableKeyFactory`, `SectionDiffEngine`, `SectionChunker`
- Publish pipeline now: PDF → parse → fingerprint → diff vs previous PUBLISHED → chunks → PUBLISHED
- Parse failure → version FAILED + operation FAILED
- `GET /api/v1/source-materials/{id}/versions/{versionId}/sections`
- UI: Parsed sections panel with change type + chunk count
- Unit tests UT-04-* green
- Embeddings stored as `pending-embed` (real OpenAI embed in later sprint)

## How to verify
1. Restart backend (Flyway V3)
2. Upload a **text** PDF (not scanned image-only)
3. Publish → COMPLETED with `sections`/`chunks` in operation result
4. Click published version → see **Parsed sections**

## Next
**Sprint S4** — PRD-05 Course authoring & structure generation

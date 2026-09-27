# PRD-04 — PDF Parsing & RAG Preparation

| Field | Value |
|--------|--------|
| Status | Done (S3) |
| Priority | P0 |
| Module | `source` + `ai` |

## 1. Problem
PDFs must become addressable sections with fingerprints and retrievable chunks.

## 2. Goals
- Parse PDF text into hierarchical sections.
- Assign **stable section keys** (`external_reference`) for cross-version identity.
- Normalize text → SHA-256 `content_hash`.
- Chunk, embed, store with metadata for RAG.
- Classify section changes vs previous published version.

## 3. Non-Goals
OCR of scanned-only PDFs (document limitation); perfect semantic sectioning of every layout.

## 4. Design
```
PDF bytes → Parser → Section tree → normalize → hash
                   → Diff vs previous version → change_type
                   → Chunker → Embeddings → Vector store
```

Normalization (deterministic): Unicode NFKC, collapse whitespace, trim, unify line endings.

Stable key strategy (v1):
1. Prefer bookmark/outline title path if present: `CH{n}/{titleSlug}`
2. Else heading heuristic + sequence: `SEC-{ordinal}-{titleSlug}`
3. Persist mapping table so renames can be detected as RENAMED when similarity high (optional stretch); MVP: same key = same section, missing key = REMOVED, new key = ADDED.

## 5. Data
```
source_sections(id, source_material_id, parent_section_id, external_reference, title, section_type)
source_section_versions(id, source_section_id, source_version_id, content, content_hash, change_type, created_at)
source_chunks(id, source_section_version_id, chunk_index, content, embedding_ref, metadata_json)
```

## 6. Acceptance Criteria
- [x] Identical re-upload → all section hashes unchanged → no MODIFIED  
- [x] Edited paragraph → MODIFIED on that section only  
- [x] Chunks carry sourceMaterialId, sourceVersionId, sectionId, language  
- [x] Parse failure marks version FAILED; no PUBLISHED event  

## 7. Unit Test Cases
See UNIT_TEST_CASES §PRD-04 (hash, normalize, diff).

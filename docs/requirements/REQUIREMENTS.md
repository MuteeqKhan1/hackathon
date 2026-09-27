# Product Requirements — v1 Production Prototype

| Field | Value |
|--------|--------|
| Document | REQ-001 |
| Status | Baseline |
| Audience | Engineering, QA, Product |

---

## 1. Problem

Organizations update authoritative learning PDFs while instructors maintain derived courses, quizzes, and explanations. Manual resync is expensive and error-prone.

## 2. Solution (v1)

AI-assisted course authoring grounded in PDF source material, with **source lineage** and **instructor-controlled synchronization** when the PDF changes.

## 3. Personas & Roles

| Role | Mock claim | Capabilities |
|------|------------|--------------|
| CONTENT_OWNER | `CONTENT_OWNER` | Upload/publish source PDFs, view impact |
| INSTRUCTOR | `INSTRUCTOR` | Create courses, generate/review/publish, sync decisions |
| STUDENT | `STUDENT` | Learn published courses, take quizzes, see progress |

## 4. In Scope (v1 Done = §106)

1. Upload Physics PDF → Source Material Version 1  
2. Create course from that source  
3. Generate chapter/topic structure (AI)  
4. Generate **Explanation** + **Quiz** per selected topics  
5. Instructor review → approve → publish  
6. Upload updated PDF → Version 2 (immutable)  
7. Detect changed sections (hash + classify)  
8. Lineage lookup → impact analysis  
9. Notify instructor (in-app)  
10. Selective regenerate → review → publish new asset versions  
11. Prior asset versions remain readable in history  
12. Student: enroll, read published explanation, take quiz, progress  

## 5. Out of Scope (v1)

- DOCX/PPTX/HTML source  
- PPT, video, video script, assignment generation  
- Adaptive learning paths / remediation engine  
- Multilingual translations  
- Real SSO/OIDC  
- Teams/Slack  
- Three-way merge UI (record conflict flag only)  
- Autonomous publish  

## 6. Functional Requirements

### FR-SRC Source Material
- FR-SRC-01 Create source material metadata under an organization.  
- FR-SRC-02 Upload PDF to object storage; create immutable version.  
- FR-SRC-03 Publish version triggers async parse + fingerprint + event.  
- FR-SRC-04 List versions; never mutate published version content.  

### FR-PARSE Document Processing
- FR-PARSE-01 Parse PDF into hierarchical sections with stable keys.  
- FR-PARSE-02 Compute normalized content hash per section.  
- FR-PARSE-03 Chunk + embed for RAG with source metadata.  

### FR-CRS Course Authoring
- FR-CRS-01 Create course bound to source material + version.  
- FR-CRS-02 AI generate structure; instructor edit/reorder.  
- FR-CRS-03 Course states: DRAFT | IN_REVIEW | PUBLISHED | UPDATE_REQUIRED | ARCHIVED.  

### FR-GEN Content Generation
- FR-GEN-01 Generate EXPLANATION and QUIZ grounded via RAG.  
- FR-GEN-02 Persist structured JSON, prompt/model versions, lineage mappings.  
- FR-GEN-03 Never publish without APPROVED status.  

### FR-REV Review & Publish
- FR-REV-01 Approve / reject / edit assets.  
- FR-REV-02 Publish course only if required assets approved (policy configurable).  

### FR-LIN Lineage
- FR-LIN-01 Query assets by source section.  
- FR-LIN-02 Relationship types: PRIMARY, SUPPORTING, REFERENCE, CUSTOM.  

### FR-SYNC Synchronization
- FR-SYNC-01 On new published version: detect ADDED/REMOVED/MODIFIED/MOVED/RENAMED/METADATA_CHANGED.  
- FR-SYNC-02 Impact levels LOW/MEDIUM/HIGH via deterministic rules.  
- FR-SYNC-03 Instructor selective actions; regenerate creates new versions.  
- FR-SYNC-04 Idempotent sync events.  
- FR-SYNC-05 MANUAL sync policy suppresses repeat recommendations.  

### FR-STU Student
- FR-STU-01 View published courses; consume explanations; attempt quizzes.  
- FR-STU-02 Persist progress and attempt scores.  

### FR-AUD Audit & Ops
- FR-AUD-01 Audit all listed domain events.  
- FR-AUD-02 correlationId on every request/job.  
- FR-AUD-03 Async operations queryable by operationId.  

## 7. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | CRUD p95 &lt; 500ms under local load |
| NFR-02 | AI/parse/sync never block HTTP thread to completion |
| NFR-03 | Org isolation enforced on all data APIs |
| NFR-04 | PDF max size configurable (default 50MB) |
| NFR-05 | Structured logs; no secrets/full PDFs |
| NFR-06 | Retry ×3 then DLQ for failed jobs |
| NFR-07 | Unit tests for domain rules mandatory |

## 8. Permissions Matrix (v1)

| Permission | Owner | Instructor | Student |
|------------|:-----:|:----------:|:-------:|
| SOURCE_* | ✓ | view if shared policy | — |
| COURSE_CREATE/EDIT/PUBLISH | — | ✓ | — |
| CONTENT_GENERATE/EDIT | — | ✓ | — |
| IMPACT_VIEW / SYNC_REVIEW | ✓ | ✓ | — |
| LESSON_VIEW / QUIZ_ATTEMPT | — | — | ✓ |
| PROGRESS_VIEW | — | own courses | own |

## 9. Acceptance of Overall Prototype

Prototype is accepted when the Physics end-to-end scenario in Architecture PRD §14 / feature PRD-09 passes manually and automated unit tests for change detection, impact, lineage, and permissions are green.

## 10. Traceability

| Requirement area | Feature PRD |
|------------------|-------------|
| Auth / roles | PRD-01 |
| Organization | PRD-02 |
| Source material | PRD-03 |
| PDF parsing / RAG | PRD-04 |
| Course authoring | PRD-05 |
| AI generation | PRD-06 |
| Review & publish | PRD-07 |
| Lineage | PRD-08 |
| Sync engine | PRD-09 |
| Student learning | PRD-10 |

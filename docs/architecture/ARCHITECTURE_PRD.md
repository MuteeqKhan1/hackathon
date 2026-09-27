# Architecture PRD — AI Adaptive Learning Platform

| Field | Value |
|--------|--------|
| Document | ARCH-PRD-001 |
| Type | System Architecture |
| Status | Approved for Prototype |
| Stack | Spring Boot · React · PostgreSQL · RabbitMQ · S3/MinIO · OpenAI |
| Scope | v1 vertical slice (§106) |

---

## 1. Purpose

Define the production-prototype architecture for an AI-powered course authoring platform whose differentiating capability is **Source Content Synchronization** with instructor-controlled updates.

This is not a demo shell. Every module must be designed for real versioning, lineage, audit, and async reliability.

---

## 2. Architecture Style

**Modular monolith** (single Spring Boot deployable) with clear package boundaries.

```
                    ┌─────────────────────────────────────┐
                    │           React SPA                 │
                    │  features/*  (role-gated routes)    │
                    └─────────────────┬───────────────────┘
                                      │ REST /api/v1
                    ┌─────────────────▼───────────────────┐
                    │         API Gateway layer           │
                    │   Auth filter · correlationId       │
                    └─────────────────┬───────────────────┘
                                      │
         ┌────────────┬───────────────┼───────────────┬────────────┐
         ▼            ▼               ▼               ▼            ▼
    source-*     course-*        content-*       sync-*      student-*
         │            │               │               │            │
         └────────────┴───────┬───────┴───────────────┴────────────┘
                              │
                    ┌─────────▼─────────┐
                    │   ai (AiProvider) │
                    │   audit           │
                    │   notification    │
                    └─────────┬─────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
         PostgreSQL       RabbitMQ        S3/MinIO
         (+ pgvector      (jobs)          (PDFs)
          optional)
```

**Why not microservices now:** one team, one deployable, strong module boundaries. Split sync/AI workers later if load demands it.

---

## 3. Bounded Contexts (Modules)

| Module | Responsibility | Owns tables |
|--------|----------------|-------------|
| `common` | IDs, errors, correlation, security helpers | — |
| `source` | Materials, versions, sections, fingerprints | `source_*` |
| `course` | Courses, chapters, topics, publish state | `courses`, `course_*` |
| `content` | Assets, asset versions, lineage mapping | `content_*` |
| `assessment` | Quizzes, questions, attempts | `quizzes`, `quiz_*` |
| `ai` | AiProvider, prompts, schema validation | `prompt_versions` |
| `sync` | Change detect, impact, sync actions | `sync_*`, `impact_*` |
| `student` | Enrollment, progress, mastery signals | `student_*` |
| `notification` | In-app + email stubs | `notifications` |
| `audit` | Append-only audit trail | `audit_events` |
| `operation` | Async operation status | `operations` |

**Dependency rule:** `sync` may read lineage via `content` services. No module reaches into another’s repositories.

---

## 4. Core Domain Invariants

1. **Immutable source versions** — publish creates a new version row; never UPDATE content of a published version.
2. **Stable section identity** — `external_reference` (or derived stable key) survives across versions so diffs are meaningful.
3. **Lineage at write time** — generation always inserts `content_source_mapping`; regeneration updates mapping for the new version.
4. **Instructor authority** — statuses: `GENERATED → PENDING_REVIEW → APPROVED → PUBLISHED` (or `REJECTED`).
5. **Selective sync** — impact is recommendation only; actions: `ACCEPT | REJECT | REGENERATE | IGNORE`.
6. **Manual override** — `syncPolicy=MANUAL` or `relationship=CUSTOM` suppresses nagging regenerate prompts.
7. **Org tenancy** — every query filtered by `organization_id`.

---

## 5. Request Flow Patterns

### 5.1 Synchronous CRUD
Auth → Controller → Service → Repository → 200/4xx  
Target: &lt; 500ms.

### 5.2 Asynchronous AI / Parse / Sync
```
POST /generate → 202 { operationId }
     → enqueue job
     → worker updates operation progress
GET /operations/{id} → INITIATED | RUNNING | COMPLETED | FAILED
```

### 5.3 Source publish → sync pipeline
```
SOURCE_VERSION_PUBLISHED
  → ChangeDetectionJob (idempotent)
  → ImpactAnalysisJob
  → NotificationJob
```

---

## 6. Technology Decisions

| Concern | Decision | Rationale |
|---------|----------|-----------|
| API | REST `/api/v1` | Simple, sufficient for v1 |
| DB | PostgreSQL 16 | Relational integrity for lineage |
| Migrations | Flyway | Reproducible schema |
| Queue | RabbitMQ | Spring AMQP, local+prod friendly |
| Files | S3 API (MinIO local) | PDF blobs out of DB |
| AI | OpenAI API via `AiProvider` | Swappable to Azure OpenAI |
| Embeddings | OpenAI embeddings → store vectors (pgvector or separate) | RAG for generation |
| Auth v1 | Header/mock JWT with role claims | Real OIDC later |
| Frontend | React + Vite + React Router | Feature folders |
| Observability | correlationId + structured JSON logs | Metric hooks later |

---

## 7. Data Model (logical)

```
organization
  └── source_material
        └── source_material_version (immutable)
              └── source_section_version (hash, change_type)
                    ↑
              source_section (stable id / external_reference)

organization
  └── course → course_chapter → course_topic
        └── content_asset → content_asset_version
              ↔ content_source_mapping ↔ source_section

sync_event → impact_analysis → sync_action
```

Full physical schema lives in Flyway; see Requirements §Schema and feature PRDs.

---

## 8. Security Architecture (v1)

- Mock authentication middleware reading `X-User-Id`, `X-Org-Id`, `X-Role`.
- Method/service-level permission checks matching PRD permission lists.
- Upload validation: PDF MIME, size limit, virus-scan hook interface (noop impl OK).
- Secrets only via env / Spring config — never committed.
- No full document or PII in logs.

---

## 9. Failure & Idempotency

- Consumers: `idempotency_key UNIQUE` on jobs/sync_events.
- Retries: 3 attempts with backoff → DLQ.
- AI failure: original published content unchanged; operation marked FAILED.
- Partial sync: per-asset action status independent.

---

## 10. Package Structure (backend)

```
com.learningplatform
├── LearningPlatformApplication
├── common
├── source
├── course
├── content
├── assessment
├── ai
├── sync
├── student
├── notification
├── audit
└── operation
```

---

## 11. Frontend Feature Map

| Feature folder | Primary roles |
|----------------|---------------|
| `source-material` | CONTENT_OWNER |
| `course-authoring` | INSTRUCTOR |
| `course-content` | INSTRUCTOR |
| `synchronization` | INSTRUCTOR |
| `student-learning` | STUDENT |
| `assessments` | INSTRUCTOR / STUDENT |
| `notifications` | ALL |

---

## 12. Quality Bar (production prototype)

- Domain logic unit-tested without network.
- Flyway migrations reviewed with PR.
- Every state change audited.
- OpenAPI (springdoc) for `/api/v1`.
- Docker Compose: Postgres, RabbitMQ, MinIO for local.

---

## 13. Explicit Non-Goals (architecture)

- Kubernetes multi-service mesh
- Event sourcing / CQRS everywhere
- Real-time collaborative editing
- Fully autonomous publish agents

---

## 14. Success Metric for Architecture

Engineering can execute the §106 vertical slice end-to-end with:

- reproducible local stack
- clear module ownership
- lineage query: “which assets depend on section X?”
- sync query: “what changed between version N and N+1?”

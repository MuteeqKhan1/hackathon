# AI Adaptive Learning Platform

Production prototype: AI-assisted course authoring with **source-to-course lineage** and **instructor-controlled synchronization** when authoritative PDFs change.

## Stack
| Layer | Choice |
|-------|--------|
| Backend | Java 17 · Spring Boot 3.4 · **Maven** |
| Frontend | React · Vite · TypeScript |
| DB | PostgreSQL 16 + Flyway |
| Queue | RabbitMQ |
| Files | MinIO (S3 API) |
| AI | `AiProvider` interface (OpenAI adapter in S5) |

## v1 Scope
PDF only · Explanation + Quiz · Mock RBAC · Sync vertical slice (§106)

## Documentation
| Doc | Purpose |
|-----|---------|
| [Architecture PRD](docs/architecture/ARCHITECTURE_PRD.md) | System design |
| [Requirements](docs/requirements/REQUIREMENTS.md) | FR/NFR |
| [Feature PRDs](docs/prds/00-index.md) | One PRD per feature |
| [Unit test cases](docs/testing/UNIT_TEST_CASES.md) | Mandatory UT catalog |
| [AI Sprint](docs/sprints/AI_SPRINT_APPROACH.md) | Delivery process |
| [AGENTS.md](AGENTS.md) | Cursor agent entry |

---

## Prerequisites
- JDK 17+
- Node.js 20+
- Docker Desktop (for Postgres / RabbitMQ / MinIO)

Maven is **not** required globally — use `backend/mvnw.cmd` (Windows) or `backend/mvnw` (Unix).

---

## Run locally (Sprint S0)

### 1. Infrastructure
```bash
docker compose up -d
```

Services:
- Postgres `localhost:5432` (postgres / postgres / learning_platform) — or your local server with same credentials
- RabbitMQ `localhost:5672` (UI :15672, alp / alp)
- MinIO `localhost:9000` (console :9001, alpminio / alpminio123)

### 2. Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run
```

- API: http://localhost:8080  
- Health: http://localhost:8080/actuator/health  
- OpenAPI: http://localhost:8080/swagger-ui.html  
- Me: `GET /api/v1/me` with headers:
  - `X-User-Id` (UUID)
  - `X-Org-Id` (UUID)
  - `X-Role` = `CONTENT_OWNER` | `INSTRUCTOR` | `STUDENT`

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 — use the role switcher; it calls `/api/v1/me` through the Vite proxy.

### 4. Tests (backend)
```bash
cd backend
.\mvnw.cmd test
```

Uses H2 + Flyway (`application-test.yml`). No Docker required for unit/API tests.

---

## Project layout
```
backend/          Spring Boot modular monolith (Maven)
frontend/         React SPA
docs/             Architecture, PRDs, tests, sprints
docker-compose.yml
.cursor/rules/    AI coding rules
```

## Status
**Sprint S3 complete** — PDF parse + section fingerprints. Next: **S4 (PRD-05)** course authoring.

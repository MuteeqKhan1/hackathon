# Sprint S0 — Foundation (Done)

## Delivered
- Maven Spring Boot modular monolith (`backend/`, **Maven only** — `mvnw` / `mvnw.cmd`)
- Mock auth: `X-User-Id`, `X-Org-Id`, `X-Role` → `PermissionGuard`
- `GET /api/v1/me`, correlation id, API error shape
- Flyway `V1__s0_foundation.sql` (orgs, users, audit, operations)
- `AiProvider` interface + placeholder impl
- Module package stubs: source, course, content, assessment, sync, student, notification, audit, operation
- React + Vite feature folders + role switcher
- `docker-compose.yml`: Postgres, RabbitMQ, MinIO
- Unit tests: Permission, PermissionGuard, MeController (13 tests green)

## Verify
```bash
cd backend && .\mvnw.cmd test
cd frontend && npm install && npm run dev
docker compose up -d   # when Docker available
cd backend && .\mvnw.cmd spring-boot:run
```

## Next
**Sprint S1** — PRD-01/02 Organization APIs (create org, membership) on this foundation.

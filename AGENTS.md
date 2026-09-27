# AGENTS.md — Cursor Agent Guide

## What we are building
Production prototype of an AI course authoring platform with **source content synchronization**. Instructor is final authority. v1 = PDF only, explanation + quiz, mock auth, Spring + React + Postgres + RabbitMQ + S3/MinIO.

## Read first
1. `docs/architecture/ARCHITECTURE_PRD.md`
2. `docs/requirements/REQUIREMENTS.md`
3. `docs/sprints/AI_SPRINT_APPROACH.md`
4. Feature PRD under `docs/prds/` for the task
5. `docs/testing/UNIT_TEST_CASES.md` for that PRD

## Workflow
PRD → unit tests → domain → API → UI. No scope beyond the active feature PRD.

## Hard rules
- Never auto-publish AI content.
- Never overwrite immutable versions.
- Always write lineage on generation.
- Always async for parse/AI/sync.
- Org isolation on every query.
- Solid design — no dummy business logic in main code paths.

## Stack
Backend `backend/` (Spring Boot), Frontend `frontend/` (React), Compose for Postgres/Rabbit/MinIO.

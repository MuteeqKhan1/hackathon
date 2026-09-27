# AI Sprint Approach

| Field | Value |
|--------|--------|
| Document | SPR-001 |
| Status | Active process |

---

## 1. Philosophy

We do **not** vibe-code a dummy LMS. Each sprint delivers a **vertical, testable slice** of real domain behavior, guided by a feature PRD.

```
PRD (what/why/AC) → Unit tests (executable AC) → Domain implementation → API/UI → Demo checklist
```

## 2. Sprint Cadence (recommended)

| Day focus | Output |
|-----------|--------|
| 0 | Confirm PRD + test cases; no feature code yet |
| 1–2 | Domain + unit tests green |
| 3 | Persistence + API + async wiring |
| 4 | UI for the feature’s primary screen(s) |
| 5 | Integration path + demo script + bugfix |

Adjust duration; **do not skip PRD or tests**.

## 3. Sprint Backlog Order (locked for v1)

| Sprint | Feature PRD | Outcome |
|--------|-------------|---------|
| S0 | Foundation | Compose, Flyway skeleton, auth mock, AGENTS/rules |
| S1 | PRD-01 + PRD-02 | Roles, org context |
| S2 | PRD-03 | Source upload + immutable versions |
| S3 | PRD-04 | PDF parse, hash, chunks |
| S4 | PRD-05 | Course + structure generation |
| S5 | PRD-06 | Explanation + quiz generation + lineage write |
| S6 | PRD-07 | Review / publish |
| S7 | PRD-08 | Lineage query APIs hardened |
| S8 | PRD-09 | Change detect → impact → notify → selective regen |
| S9 | PRD-10 | Student consume + quiz + progress |
| S10 | Hardening | E2E script, idempotency, audit completeness |
| S11 | PRD-11 | Video lesson storyboard generation + student player |
| S12 | PRD-12 + PRD-13 | Multilingual + learning pace preferences |
| S13 | PRD-14 | Auto-generate drafts on course create |

## 4. Per-Feature Definition of Done

Copied into every PRD; enforce in review:

1. Acceptance criteria checked  
2. Unit tests listed in PRD pass  
3. Audit events for mutations  
4. Org isolation verified  
5. No scope creep beyond PRD  
6. README/demo notes updated if user-visible  

## 5. Agent / Cursor Rules of Engagement

- Always open the feature PRD before coding.  
- Prefer changing domain services over “quick UI fakes” of business rules.  
- If a requirement is missing, update the PRD first, then code.  
- Fake LLM in tests; real LLM only behind `AiProvider` in running app.

## 6. Demo Script (final prototype)

Use Physics PDF v1 → course → explanation/quiz → publish → PDF v2 → sync UI → regenerate quiz → student attempt. See `docs/sprints/SPRINT_VERTICAL_SLICE.md`.

# Sprint S9 — Student Learning (Done)

## Delivered (PRD-10)
- Flyway `V7__student_learning.sql`
- `StudentCourseQuery` — PUBLISHED only (DRAFT hidden)
- `QuizScorer` — deterministic MCQ %
- `ProgressService` / `MasteryService` (no slow/fast labels)
- APIs under `/api/v1/student/...`
- UI: **Student Learning** page
- Unit tests UT-10-01…06

## Try it
1. Restart backend (Flyway V7)
2. Role **STUDENT**, org Physics Academy
3. Open **Student Learning** → published course **test** → topic → read explanation → submit quiz
4. Confirm progress % and score banner

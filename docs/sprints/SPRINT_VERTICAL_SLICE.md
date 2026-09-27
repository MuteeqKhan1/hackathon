# Sprint — Vertical Slice Demo (§106)

## Goal
Prove production-shaped spine, not UI mocks:

**PDF → Course → Explanation+Quiz → Lineage → Source v2 → Impact → Selective Regen → Publish → History → Student quiz**

## Demo checklist
1. Seed org + users (owner, instructor, student).
2. Owner uploads `physics-v1.pdf`, publishes.
3. Sections + hashes visible.
4. Instructor creates “Physics Fundamentals”, generates structure.
5. Generate explanation + quiz for Newton’s Second Law topic.
6. Approve assets; publish course.
7. Owner uploads `physics-v2.pdf` (formula/example changed); publish.
8. Sync dashboard shows HIGH/MEDIUM impacts with reasons.
9. Instructor regenerates explanation + quiz only; skips one asset.
10. Approve new versions; publish; open version history (v1 still there).
11. Student attempts new quiz; progress updates.

## Exit criteria
All UT-09-* and lineage tests green; manual checklist above recorded.

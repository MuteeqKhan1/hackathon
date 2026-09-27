# PRD-10 — Student Learning (Basic)

| Field | Value |
|--------|--------|
| Status | Ready |
| Priority | P1 (after sync spine) |
| Module | `student` + `assessment` |

## 1. Problem
Validate published content is consumable; capture progress signals for future adaptive learning.

## 2. Goals
- Enroll / list published courses for student in org.
- Read published explanation for topic.
- Attempt quiz; score; persist attempts.
- Course/chapter/topic progress percentages.

## 3. Non-Goals
Adaptive path engine, remediation loops, recommendations ML (Phase 2).

## 4. Data
```
student_course_enrollments(...)
student_topic_progress(...)
student_quiz_attempts(id, student_id, quiz_id, score, answers_json, created_at)
student_mastery(student_id, topic_id, mastery_score, attempts, average_quiz_score)
```

## 5. API
- `GET /api/v1/student/courses`
- `GET /api/v1/student/courses/{courseId}`
- `GET /api/v1/student/topics/{topicId}`
- `POST /api/v1/student/quizzes/{quizId}/attempts`
- `GET /api/v1/student/progress/{courseId}`

## 6. Acceptance Criteria
- [ ] Draft courses invisible to students  
- [ ] Quiz attempt scoring deterministic for MCQ  
- [ ] Progress updates after lesson view / quiz  
- [ ] No permanent “slow/fast” student label fields  

## 7. Unit Test Cases
See UNIT_TEST_CASES §PRD-10.

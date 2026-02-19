# AI Annotation Summary Design

## Problem

Teachers create per-item annotations in Learn mode (per word in a set) and Tutorial mode (per letter in a category). The StudentDetailsScreen shows annotation cards for each set/category, but the card text shows raw annotation notes rather than a synthesized view. Teachers need a concise AI-generated summary that highlights patterns, strengths, challenges, and recommendations across all items in a set or category.

## Decision: Approach A — New AnnotationSummary Table + Gemini Generation

Chosen over adding a summary field to the existing annotation entity (conceptually messy, mixes item-level and set-level data) and in-memory-only caching (requires internet every session, contradicts persistence requirement).

## Design

### Data Layer

**New Room Entity: `AnnotationSummary`**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| studentId | String | Links to student |
| setId | Long | Matches LearnerProfileAnnotation.setId |
| sessionMode | String | "LEARN" or "TUTORIAL" |
| summaryText | String | AI-generated summary (2-4 sentences) |
| generatedAt | Long | Timestamp of generation |

Unique constraint on (studentId, setId, sessionMode) — one summary per set/category per student.

**New DAO: `AnnotationSummaryDao`**

- `insertOrUpdate(summary)` — upsert by unique constraint
- `getSummary(studentId, setId, sessionMode)` — fetch one summary
- `getSummariesForStudent(studentId)` — fetch all summaries for a student
- `deleteSummary(studentId, setId, sessionMode)` — for cleanup

**DB Migration:** Version 11 to 12, adds `annotation_summary` table.

### AI Layer

**New method in `GeminiRepository`: `generateAnnotationSummary()`**

Input: All `LearnerProfileAnnotation` items for a given (studentId, setId, sessionMode), formatted as structured data including item name, level of progress, strengths (tags + note), and challenges (tags + note).

Prompt structure:
```
You are an educational assessment assistant. Based on the following
teacher annotations for a student's [learn/tutorial] session,
generate a concise 2-4 sentence summary.

The summary should:
- Highlight overall strengths and areas for improvement
- Note specific patterns across items
- Provide one actionable recommendation for the teacher

Annotations:
[formatted annotation data]
```

Model: Gemini 2.5 Flash Lite (same as existing integration).

### Trigger

- After teacher saves/updates an annotation in LearnModeSessionScreen or TutorialSessionScreen
- ViewModel calls GeminiRepository.generateAnnotationSummary() asynchronously
- Result stored via AnnotationSummaryDao.insertOrUpdate()
- Silent failure — if Gemini call fails, summary retains previous value (or stays empty if first time). No error shown to teacher. Retries on next annotation save.

### UI Changes

**LearnAnnotationCard and TutorialAnnotationCard:**
- Annotation note text replaced with AI-generated summary from AnnotationSummary
- Tags (strengths/challenges) remain unchanged
- Fallback: if no summary exists yet, show "Summary generating..." placeholder or original annotation text

**StudentDetailsScreen data flow:**
1. ClassroomViewModel.loadStudentDetails() fetches annotations grouped by set/category (unchanged)
2. Additionally fetches AnnotationSummary for each set/category via AnnotationSummaryDao.getSummariesForStudent(studentId)
3. Summary text passed into card composables as the annotation text parameter

**Detail screens unchanged:** LearnAnnotationDetailsScreen and TutorialAnnotationDetailsScreen continue to show full per-item annotations.

### Files to Modify/Create

| Action | File |
|--------|------|
| Create | `AnnotationSummary.kt` (entity) |
| Create | `AnnotationSummaryDao.kt` (DAO) |
| Modify | `AppDatabase.kt` (add entity, migration, DAO getter) |
| Modify | `GeminiRepository.kt` (add summary generation method) |
| Modify | `ClassroomViewModel.kt` (trigger generation, load summaries) |
| Modify | `StudentDetailsScreen.kt` (pass summary to cards) |
| Modify | `LearnAnnotationCard.kt` (accept/display summary) |
| Modify | `TutorialAnnotationCard.kt` (accept/display summary) |
| Modify | `LearnModeSessionScreen.kt` (trigger summary after annotation save) |
| Modify | `TutorialSessionScreen.kt` (trigger summary after annotation save) |

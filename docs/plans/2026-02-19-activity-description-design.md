# Design: AI-Generated Activity Description

**Date:** 2026-02-19
**Branch:** update/Register-and-Login

## Summary

Add an AI-generated description/label under the main title in TutorialAnnotationDetailsScreen and LearnAnnotationDetailsScreen. The description explains what the activity/lesson covers (not student performance). Generated via Gemini on first visit, cached in a new DB table for instant subsequent loads.

## Approach

New `activity_description_cache` table + new GeminiRepository method (Approach A). Generate once, cache in DB.

## Data Layer

### New Entity: ActivityDescriptionCache
- Table: `activity_description_cache`
- Fields: id (PK), setId, sessionMode ("LEARN"/"TUTORIAL"), activityId (0 for tutorials), descriptionText, generatedAt
- Unique index on (setId, sessionMode, activityId)

### New DAO: ActivityDescriptionCacheDao
- `getDescription(setId, sessionMode, activityId): ActivityDescriptionCache?`
- `insertOrUpdate(cache: ActivityDescriptionCache): Long`

### AppDatabase
- Bump version 15 -> 16
- Add ActivityDescriptionCache entity and DAO
- Destructive migration already in place

### GeminiRepository
- New method: `generateActivityDescription(activityTitle: String, sessionMode: String, items: List<String>, configurations: List<String>?): String?`
- Prompt: Generate 1-2 sentence description of the lesson/activity
- For tutorials: describes the letter set being practiced
- For learn mode: describes the words and their configuration types

## UI Layer

### Visual Placement
```
Tutorial Mode          (or "Learn Mode")
Uppercase Vowels       (main title)
[AI description here]  (new - 14sp, gray, centered, italic)
```

### TutorialAnnotationDetailsScreen
- Add `descriptionText` state variable
- In LaunchedEffect: check cache -> if miss, call Gemini -> save to cache -> update state
- Show "Generating description..." while loading
- Display description below main title

### LearnAnnotationDetailsScreen
- Same pattern as Tutorial screen
- Also passes word configuration types for richer description

### Error Handling
- Silent failure: if Gemini call fails, show nothing (matches existing annotation summary behavior)

## Files Affected

1. New: `data/entity/ActivityDescriptionCache.kt`
2. New: `data/dao/ActivityDescriptionCacheDao.kt`
3. Modify: `data/AppDatabase.kt` (version bump, add entity + DAO)
4. Modify: `data/repository/GeminiRepository.kt` (new method)
5. Modify: `ui/feature/classroom/TutorialAnnotationDetailsScreen.kt`
6. Modify: `ui/feature/classroom/LearnAnnotationDetailsScreen.kt`

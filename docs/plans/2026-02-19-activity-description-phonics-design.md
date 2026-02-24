# Design: Phonics-Aware Activity Descriptions

**Date:** 2026-02-19
**Branch:** update/Register-and-Login

## Problem

Current Learn mode activity descriptions are generic. For words like "dog", "bob", "bib" with fill-in-the-blanks, the AI generates:

> "Students practice reading and writing the words dog, bob, and bib using fill-in-the-blanks."

This doesn't mention the phonics relationship between the words (word families, shared vowels, onset consonants) or what letter positions are being practiced.

## Goal

When a clear phonics pattern exists (word family, same vowel, same onset), the description should mention it. When no pattern exists, keep it simple.

**Example improved output:**
> "Students practice short-o CVC words bob and dog through fill-in-the-blanks, focusing on medial vowel recognition."

## Approach

Enrich the existing Gemini prompt with CVC analysis data that already exists in `GeminiRepository`.

## Changes

### 1. GeminiRepository.generateActivityDescription

Add two new optional parameters:
- `selectedLetterIndices: List<Int>?` - the blanked letter index per word
- `phonicsContext: String?` - pre-built description of the phonics pattern

Update the system instruction prompt to:
- Include phonics context when provided
- Add examples showing pattern-aware descriptions
- Instruct: "If a phonics pattern is provided, mention it naturally. If no pattern exists, describe the activity simply."

### 2. LearnAnnotationDetailsScreen.kt

Before calling `generateActivityDescription`:
- Collect `selectedLetterIndex` from each `SetWord`
- Run CVC analysis on the loaded words (reuse `GeminiRepository` analysis logic or inline simple detection)
- Build a `phonicsContext` string describing detected patterns
- Pass both to the description generator

### 3. Cache Invalidation

Clear existing Learn mode cached descriptions (via a DAO method `deleteBySessionMode("LEARN")`) so they regenerate with the improved prompt.

### 4. TutorialAnnotationDetailsScreen.kt

No changes. Tutorial descriptions are already adequate.

## Files Affected

1. Modify: `data/repository/GeminiRepository.kt` (update `generateActivityDescription` signature and prompt)
2. Modify: `ui/feature/classroom/LearnAnnotationDetailsScreen.kt` (build phonics context, pass to generator)
3. Modify: `data/dao/ActivityDescriptionCacheDao.kt` (add `deleteBySessionMode` method)

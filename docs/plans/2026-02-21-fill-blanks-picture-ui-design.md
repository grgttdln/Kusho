# Fill in the Blanks — Picture UI Design

**Date:** 2026-02-21

## Problem

When a Fill in the Blank exercise has an associated picture, it currently renders inside the standard card-wrapped layout (white card with purple border, image inside the card, masked word below). This looks different from the Name the Picture layout, which shows a large standalone image card with the word display below it.

## Goal

When Fill in the Blank has a picture, use the Name the Picture visual layout (big standalone image card + word below). When there's no picture, keep the current card-wrapped layout. Exercise logic (single masked letter) stays unchanged.

## Approach

Branch at the top level in `LearnModeSessionScreen.kt`.

### Changes

**File:** `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt`

1. **Branching condition (~line 1111-1114):** Change `if (!isNameThePicture)` to `if (!isNameThePicture && !isFillInBlankWithImage)`, where `isFillInBlankWithImage` is derived from `configurationType == "Fill in the Blank"` and `imageExists`.

2. **Else branch (~line 1283-1387):** The existing Name the Picture layout branch now also handles Fill in the Blank with image. In the word display section, conditionally render `FillInTheBlankDisplay` (for Fill in the Blank) or `NameThePictureDisplay` (for Name the Picture).

### What does NOT change

- `FillInTheBlankDisplay` composable
- `NameThePictureDisplay` composable
- ViewModel / data models
- Wear side
- Exercise logic (single masked letter for Fill in the Blank)

## Result

Fill in the Blank with picture: big standalone image card on top, masked word (e.g., "D _ G") below.
Fill in the Blank without picture: current card-wrapped layout with masked word.

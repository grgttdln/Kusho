# Guaranteed Unique Titles Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Ensure AI-generated activity and set titles are never exact duplicates of existing titles, while preserving the semantic similarity banners.

**Architecture:** Strengthen AI prompts to forbid exact duplicates and request an `alternateTitle` fallback. Add a deterministic dedup safety net in `parseStep3Response` that uses the AI's alternate first, then falls back to suffix-based renaming. Thread existing title lists from `stepAssignConfigurations` into the parser.

**Tech Stack:** Kotlin, Jetpack Compose, Gemini SDK, Gson

---

### Task 1: Add `alternateTitle` to Data Models

**Files:**
- Modify: `app/src/main/java/com/example/app/data/model/GeminiApiModels.kt:33-36`
- Modify: `app/src/main/java/com/example/app/data/model/AiGeneratedData.kt:20-24`

**Step 1: Add `alternateTitle` to `TitleSimilarityResponse`**

In `GeminiApiModels.kt`, change:

```kotlin
data class TitleSimilarityResponse(
    val similarTo: String = "",
    val reason: String = ""
)
```

To:

```kotlin
data class TitleSimilarityResponse(
    val similarTo: String = "",
    val reason: String = "",
    val alternateTitle: String = ""
)
```

**Step 2: Add `alternateTitle` to `TitleSimilarity`**

In `AiGeneratedData.kt`, change:

```kotlin
data class TitleSimilarity(
    val generatedTitle: String,
    val similarToExisting: String,
    val reason: String
)
```

To:

```kotlin
data class TitleSimilarity(
    val generatedTitle: String,
    val similarToExisting: String,
    val reason: String,
    val alternateTitle: String = ""
)
```

**Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 2: Strengthen Prompts with No-Duplicate Rule and `alternateTitle`

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`
  - `stepGroupIntoSets` (line ~281)
  - `stepRegroupSet` (line ~349)
  - `stepAssignConfigurations` (line ~429)

**Step 1: Update `stepGroupIntoSets` prompt (line ~281)**

Change the `existingSetTitlesSection` block from:

```kotlin
val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
    """

EXISTING SET TITLES (avoid generating titles semantically similar to these):
${existingSetTitles.joinToString("\n") { "- $it" }}

If a generated set title is semantically similar to an existing one, include a "titleSimilarity" field in that set's JSON object with "similarTo" (the existing title it resembles) and "reason" (one-sentence explanation of why they are similar).
"""
} else ""
```

To:

```kotlin
val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
    """

EXISTING SET TITLES:
${existingSetTitles.joinToString("\n") { "- $it" }}

CRITICAL: Generated set titles MUST NOT be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.

If a generated set title is semantically similar to an existing one (even if not identical), include a "titleSimilarity" field in that set's JSON object with "similarTo" (the existing title it resembles), "reason" (one-sentence explanation), and "alternateTitle" (a different 15-char-max title that avoids the similarity).
"""
} else ""
```

Also update the JSON response format description at the end of the system instruction, changing:

```
and optionally "titleSimilarity" (object with "similarTo" and "reason" strings)
```

To:

```
and optionally "titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings)
```

**Step 2: Update `stepRegroupSet` prompt (line ~349)**

Same changes as Step 1 — change the `existingSetTitlesSection` block from:

```kotlin
val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
    """

EXISTING SET TITLES (avoid generating titles semantically similar to these):
${existingSetTitles.joinToString("\n") { "- $it" }}

If the generated set title is semantically similar to an existing one, include a "titleSimilarity" field with "similarTo" (the existing title it resembles) and "reason" (one-sentence explanation of why they are similar).
"""
} else ""
```

To:

```kotlin
val existingSetTitlesSection = if (existingSetTitles.isNotEmpty()) {
    """

EXISTING SET TITLES:
${existingSetTitles.joinToString("\n") { "- $it" }}

CRITICAL: The generated set title MUST NOT be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.

If the generated set title is semantically similar to an existing one (even if not identical), include a "titleSimilarity" field with "similarTo" (the existing title it resembles), "reason" (one-sentence explanation), and "alternateTitle" (a different 15-char-max title that avoids the similarity).
"""
} else ""
```

Also update the JSON response format description:

```
and optionally "titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings) fields
```

**Step 3: Update `stepAssignConfigurations` prompt (line ~429)**

Change the `existingTitlesSection` block. Replace the line:

```kotlin
appendLine("If a generated title (activity or set) is semantically similar to an existing one, include a \"titleSimilarity\" field in that object with \"similarTo\" (the existing title) and \"reason\" (one-sentence explanation).")
```

With:

```kotlin
appendLine("CRITICAL: Neither the activity title nor any set title may be identical (case-insensitive) to any existing title listed above. If the best title matches an existing one exactly, choose a different but related title.")
appendLine()
appendLine("If a generated title (activity or set) is semantically similar to an existing one (even if not identical), include a \"titleSimilarity\" field in that object with \"similarTo\" (the existing title), \"reason\" (one-sentence explanation), and \"alternateTitle\" (a different 15-char-max title that avoids the similarity).")
```

Also update the two JSON response format descriptions in the system instruction. Change both occurrences of:

```
"titleSimilarity" (object with "similarTo" and "reason" strings)
```

To:

```
"titleSimilarity" (object with "similarTo", "reason", and "alternateTitle" strings)
```

(There are two — one for activity, one for sets.)

**Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 3: Add Dedup Safety Net in `parseStep3Response`

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`
  - `stepAssignConfigurations` (line ~502) — pass existing titles to parser
  - `parseStep3Response` (line ~541) — add dedup logic

**Step 1: Update `parseStep3Response` signature**

Change from:

```kotlin
private fun parseStep3Response(
    response: AiResponse,
    availableWords: List<Word>
): AiGenerationResult {
```

To:

```kotlin
private fun parseStep3Response(
    response: AiResponse,
    availableWords: List<Word>,
    existingActivityTitles: List<String> = emptyList(),
    existingSetTitles: List<String> = emptyList()
): AiGenerationResult {
```

**Step 2: Add `ensureUniqueTitle` helper function**

Add this private function to `GeminiRepository`, right before `parseStep3Response` (or after it, alongside other helpers):

```kotlin
/**
 * Ensures a title is unique against a set of existing titles (case-insensitive).
 * Tries alternateTitle first, then appends suffixes " II", " III", etc.
 * Respects the 15-character max limit.
 */
private fun ensureUniqueTitle(
    title: String,
    alternateTitle: String,
    existingTitles: Set<String>,
    usedTitles: MutableSet<String>
): String {
    val lowerExisting = existingTitles.map { it.lowercase() }.toSet()
    val lowerUsed = usedTitles.map { it.lowercase() }.toSet()
    val allTaken = lowerExisting + lowerUsed

    // If title is already unique, use it
    if (title.lowercase() !in allTaken) {
        usedTitles.add(title)
        return title
    }

    // Try AI-provided alternate title
    if (alternateTitle.isNotBlank() && alternateTitle.length <= 15 &&
        alternateTitle.lowercase() !in allTaken
    ) {
        usedTitles.add(alternateTitle)
        return alternateTitle
    }

    // Suffix fallback
    val suffixes = listOf(" II", " III", " IV", " V")
    for (suffix in suffixes) {
        val candidate = if (title.length + suffix.length <= 15) {
            title + suffix
        } else {
            title.take(15 - suffix.length) + suffix
        }
        if (candidate.lowercase() !in allTaken) {
            usedTitles.add(candidate)
            return candidate
        }
    }

    // Last resort: truncate and add number
    val candidate = title.take(12) + " " + (existingTitles.size + usedTitles.size)
    usedTitles.add(candidate)
    return candidate.take(15)
}
```

**Step 3: Add dedup logic in `parseStep3Response`**

After the activity title is computed (the `val activityTitle = when { ... }` block, around line ~556) and before extracting `activityTitleSimilarity`, add:

```kotlin
// Build lookup sets for dedup
val existingActivityTitlesLower = existingActivityTitles.map { it.lowercase() }.toSet()
val existingSetTitlesLower = existingSetTitles.map { it.lowercase() }.toSet()
val usedActivityTitles = mutableSetOf<String>()
val usedSetTitles = mutableSetOf<String>()
```

Then wrap the activity title in dedup. Replace:

```kotlin
val activityTitle = when { ... }
```

Keep that block as-is, but add right after it:

```kotlin
// Dedup activity title against existing activity titles
val activityAlternate = response.activity.titleSimilarity?.alternateTitle ?: ""
val dedupedActivityTitle = ensureUniqueTitle(
    activityTitle, activityAlternate,
    existingActivityTitlesLower.map { existingActivityTitles.first { t -> t.lowercase() == it } }.toSet(),
    usedActivityTitles
)
```

Wait — that's overcomplicating. Let me simplify. The `ensureUniqueTitle` function already takes lowercase internally. Just pass the original-case sets:

```kotlin
val dedupedActivityTitle = ensureUniqueTitle(
    activityTitle,
    response.activity.titleSimilarity?.alternateTitle ?: "",
    existingActivityTitles.toSet(),
    usedActivityTitles
)
```

Then use `dedupedActivityTitle` instead of `activityTitle` for the rest of the function. Specifically, update the `activityTitleSimilarity` extraction to use `dedupedActivityTitle`:

```kotlin
val activityTitleSimilarity = response.activity.titleSimilarity?.let { sim ->
    if (sim.similarTo.isNotBlank()) {
        TitleSimilarity(
            generatedTitle = dedupedActivityTitle,
            similarToExisting = sim.similarTo,
            reason = sim.reason,
            alternateTitle = sim.alternateTitle
        )
    } else null
}
```

And update the final return to use `dedupedActivityTitle`:

```kotlin
return AiGenerationResult.Success(
    AiGeneratedActivity(
        activity = AiActivityInfo(
            title = dedupedActivityTitle,
            ...
```

**Step 4: Add set title dedup in the `for ((index, set) in response.sets.withIndex())` loop**

After the `setTitle` is computed (the `val setTitle = when { ... }` block), add:

```kotlin
// Dedup set title against existing set titles and other generated sets
val dedupedSetTitle = ensureUniqueTitle(
    setTitle,
    set.titleSimilarity?.alternateTitle ?: "",
    existingSetTitles.toSet(),
    usedSetTitles
)
```

Then use `dedupedSetTitle` instead of `setTitle` for the rest of the loop body. Specifically update:
- The Log warning: `"Set '${dedupedSetTitle}' has only ..."`
- The `TitleSimilarity` construction: `generatedTitle = dedupedSetTitle`
- The `AiGeneratedSet` construction: `title = dedupedSetTitle`

**Step 5: Pass existing titles from `stepAssignConfigurations` to `parseStep3Response`**

In `stepAssignConfigurations`, change the last line from:

```kotlin
return parseStep3Response(aiResponse, availableWords)
```

To:

```kotlin
return parseStep3Response(aiResponse, availableWords, existingActivityTitles, existingSetTitles)
```

**Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 4: Update `TitleSimilarity` Construction to Include `alternateTitle`

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt` — anywhere `TitleSimilarity(...)` is constructed

**Step 1: Verify all `TitleSimilarity` constructions pass `alternateTitle`**

There are two places in `parseStep3Response` where `TitleSimilarity` is constructed:

1. Activity-level (around line ~559): Already updated in Task 3 Step 3.
2. Set-level (around line ~639): Update from:

```kotlin
TitleSimilarity(
    generatedTitle = setTitle,
    similarToExisting = sim.similarTo,
    reason = sim.reason
)
```

To:

```kotlin
TitleSimilarity(
    generatedTitle = dedupedSetTitle,
    similarToExisting = sim.similarTo,
    reason = sim.reason,
    alternateTitle = sim.alternateTitle
)
```

(This should already be done as part of Task 3 Step 4, but verify.)

**Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 5: Final Build and Manual Smoke Test

**Step 1: Full clean build**

Run: `./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL with no new warnings related to our changes.

**Step 2: Verification checklist**

Verify by reading the code:
- [ ] `TitleSimilarityResponse` has `alternateTitle` field
- [ ] `TitleSimilarity` has `alternateTitle` field
- [ ] All 3 prompts (Step 2, Step 2-regen, Step 3) contain the "MUST NOT be identical" rule
- [ ] All 3 prompts request `alternateTitle` in the `titleSimilarity` response format
- [ ] `parseStep3Response` accepts `existingActivityTitles` and `existingSetTitles`
- [ ] `ensureUniqueTitle` helper exists and handles: already-unique, alternate title, suffix fallback
- [ ] Activity title is deduped before being used in the return value
- [ ] Each set title is deduped, including against other generated sets in the same batch
- [ ] `stepAssignConfigurations` passes both title lists to `parseStep3Response`

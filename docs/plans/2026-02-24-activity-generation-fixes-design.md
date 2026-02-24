# Activity Generation Fixes Design

## Problem Statement

Two issues in the Activity Generation pipeline:

1. **Insufficient Word Bank Fallback**: When the word bank has fewer than 3 words matching a teacher's pattern, the system falls back to using ALL words. This causes the AI to ignore the pattern entirely.
2. **AI Prompt Adherence Failure**: The AI frequently ignores structural constraints (e.g., prompting for "-un" words produces "map" or "sad") even when sufficient matching words exist.

## Current System

The Activity Generation uses a 3-step Gemini pipeline in `GeminiRepository.kt`:

- **Step 1 (Filter)**: AI receives all Word Bank words with CVC analysis, selects words matching the teacher's prompt
- **Step 2 (Group)**: AI picks a coherent subset of 3-10 words
- **Step 3 (Configure)**: AI assigns activity types to each word

The current fallback when Step 1 returns <3 words: use the entire word bank, losing all pattern fidelity.

## Design

### Component 1: Pattern Router

A classifier that determines whether the teacher's prompt requires structural (code-based) or semantic (AI-based) filtering.

**Pattern types:**

| Type | Example Prompt | Detection | Filter |
|------|---------------|-----------|--------|
| Rime/ending | "-un words", "ending in -at" | Regex: `-[a-z]{2}`, `ending in` | `CVCAnalysis.rime` |
| Onset/start | "words starting with b" | Regex: `starting with [bcdfghjklmnprstvwxyz]` | `CVCAnalysis.onset` |
| Vowel sound | "short a words" | Regex: vowel references | `CVCAnalysis.vowel` |
| Coda | "words ending in t" (single consonant) | Regex: single consonant ending | `CVCAnalysis.coda` |
| Semantic | "animal words" | No structural pattern detected | Route to AI Step 1 |

**Flow:**

```
Teacher prompt → PatternRouter.classify(prompt)
  ├─ Structural → Code-based filter using CVC analysis
  │   ├─ >= 3 matches → proceed to Step 2
  │   └─ < 3 matches → Inline Suggestion flow
  └─ Semantic → AI Step 1 + post-validation safety net
```

**Location**: Private utility in `GeminiRepository.kt`, where CVC analysis already exists.

### Component 2: Inline Word Suggestion Flow

When fewer than 3 words match a structural pattern, generation pauses and presents suggestions.

**State:**

```kotlin
data class InsufficientWords(
    val pattern: String,           // e.g., "-un"
    val matchingWords: List<String>, // words already in bank
    val suggestedWords: List<String>, // AI-generated suggestions
    val needed: Int                // minimum needed (3)
)
```

**UI dialog:**

```
"Only 2 words in your Word Bank match '-un' (fun, run).
 You need at least 3. Here are some suggestions:"
 ☑ bun  ☑ sun  ☑ nun  ☐ gun  ☐ pun
 [Add Selected & Generate]  [Cancel]
```

**Suggestion generation**: Reuse existing `generateCVCWords()` with the detected pattern as prompt, excluding words already in the bank.

**After teacher selects**: Words are added to Word Bank via `WordRepository`, then `createActivity()` re-invokes with the expanded word list.

**Edge case**: If `generateCVCWords()` can't produce suggestions, show a message asking the teacher to add words manually.

### Component 3: Post-Validation Safety Net

For semantic requests that also contain a structural constraint (e.g., "animal words ending in -at"), a post-validation layer strips non-matching words after AI Step 1.

```kotlin
fun postValidate(
    aiFilteredWords: List<String>,
    pattern: DetectedPattern?,
    analyses: Map<String, CVCAnalysis>
): List<String> {
    if (pattern == null) return aiFilteredWords
    return aiFilteredWords.filter { word ->
        val analysis = analyses[word] ?: return@filter false
        pattern.matches(analysis)
    }
}
```

Only activates when PatternRouter detects a structural component in an otherwise semantic request.

## Error Handling

| Scenario | Behavior |
|----------|----------|
| 0 matches + AI suggestions fail | "No CVC words match this pattern. Try a different pattern or add words manually." |
| Teacher cancels suggestion dialog | Return to activity creation screen |
| Network error during suggestions | Show error, offer retry or manual add |
| Pattern router misclassifies | AI fallback still works; post-validation catches violations |

## Testing Strategy

- Unit tests for `PatternRouter.classify()` with various prompt formats
- Unit tests for code-based CVC filtering against known word lists
- Unit tests for post-validation logic
- Integration test for full flow: insufficient words → suggest → add → generate

## Files Modified

| File | Change |
|------|--------|
| `GeminiRepository.kt` | Add PatternRouter, code-based filtering, post-validation |
| `LessonViewModel.kt` | Add InsufficientWords state, suggestion flow, auto-resume |
| `GenerateActivityViewModel.kt` | May need state changes for suggestion dialog |
| Learn screen composables | Add suggestion dialog UI |
| `WordRepository.kt` | Possibly add batch-add convenience method |

# Phonics-Aware Activity Descriptions Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Improve Learn mode activity descriptions to mention phonics patterns (word families, shared vowels, onset consonants) when they exist, so teachers understand why words are grouped together.

**Architecture:** Enrich the existing `generateActivityDescription` Gemini prompt with CVC analysis data. The CVC analysis logic already exists in `GeminiRepository` but is currently private and only used for activity generation. We'll expose a public helper to build phonics context, then pass it from the Learn screen into the description generator. We'll also clear stale cached Learn descriptions.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Gemini API (gemini-2.5-flash-lite)

---

### Task 1: Add `deleteBySessionMode` to ActivityDescriptionCacheDao

**Files:**
- Modify: `app/src/main/java/com/example/app/data/dao/ActivityDescriptionCacheDao.kt`

**Step 1: Add the delete query method**

Add this method inside the `ActivityDescriptionCacheDao` interface, after the existing `getDescription` method:

```kotlin
@Query("DELETE FROM activity_description_cache WHERE sessionMode = :sessionMode")
suspend fun deleteBySessionMode(sessionMode: String)
```

---

### Task 2: Add public phonics context builder to GeminiRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt`

**Step 1: Add a public method that builds phonics context from word names**

Add this public method in the `GeminiRepository` class, in the `// ========== Activity Description ==========` section (before the existing `generateActivityDescription` method, around line 321):

```kotlin
/**
 * Build a phonics context string from a list of word names and their selected letter indices.
 * Used to enrich activity description generation with CVC pattern info.
 */
fun buildPhonicsContext(
    wordNames: List<String>,
    selectedLetterIndices: List<Int>,
    configurations: List<String>
): String {
    val vowels = setOf('a', 'e', 'i', 'o', 'u')

    data class SimpleAnalysis(
        val word: String,
        val onset: Char,
        val vowel: Char,
        val coda: Char,
        val rime: String
    )

    val analyses = wordNames.map { w ->
        val lower = w.lowercase()
        SimpleAnalysis(
            word = w,
            onset = lower.getOrElse(0) { ' ' },
            vowel = lower.getOrElse(1) { ' ' },
            coda = lower.getOrElse(2) { ' ' },
            rime = if (lower.length >= 2) lower.drop(1) else ""
        )
    }

    val sb = StringBuilder()

    // Detect word families (same rime, 2+ words)
    val wordFamilies = analyses.groupBy { it.rime }.filter { it.value.size >= 2 }
    if (wordFamilies.isNotEmpty()) {
        wordFamilies.forEach { (rime, words) ->
            sb.appendLine("Word family: -$rime (${words.joinToString(", ") { it.word }})")
        }
    }

    // Detect same vowel groups (only if no word family covers all words)
    val familyWords = wordFamilies.values.flatten().map { it.word }.toSet()
    val nonFamilyWords = analyses.filter { it.word !in familyWords }
    if (nonFamilyWords.isNotEmpty()) {
        val sameVowel = analyses.groupBy { it.vowel }.filter { it.value.size >= 2 }
        sameVowel.forEach { (vowel, words) ->
            sb.appendLine("Shared vowel sound: short '$vowel' (${words.joinToString(", ") { it.word }})")
        }
    }

    // Detect same onset consonant groups
    val sameOnset = analyses.groupBy { it.onset }.filter { it.value.size >= 2 }
    if (sameOnset.isNotEmpty() && wordFamilies.isEmpty()) {
        sameOnset.forEach { (onset, words) ->
            sb.appendLine("Same starting letter: '$onset' (${words.joinToString(", ") { it.word }})")
        }
    }

    // Describe what letter positions are being practiced in fill-in-the-blanks
    val fillIndices = wordNames.zip(configurations).zip(selectedLetterIndices)
        .filter { (pair, _) -> pair.second.lowercase() in listOf("fill in the blanks", "fill in the blank") }
        .map { (pair, idx) -> Triple(pair.first, pair.second, idx) }

    if (fillIndices.isNotEmpty()) {
        val positionGroups = fillIndices.groupBy { it.third }
        val positionDescriptions = positionGroups.map { (idx, words) ->
            val posName = when (idx) {
                0 -> "onset (first letter)"
                1 -> "medial vowel (middle letter)"
                2 -> "coda (last letter)"
                else -> "letter index $idx"
            }
            "$posName: ${words.joinToString(", ") { it.first }}"
        }
        sb.appendLine("Fill-in-the-blanks targets: ${positionDescriptions.joinToString("; ")}")
    }

    return sb.toString().trimEnd()
}
```

---

### Task 3: Update `generateActivityDescription` to accept and use phonics context

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/GeminiRepository.kt:322-395`

**Step 1: Add `phonicsContext` parameter**

Change the method signature from:

```kotlin
suspend fun generateActivityDescription(
    activityTitle: String,
    sessionMode: String,
    items: List<String>,
    configurations: List<String>? = null
): String? = withContext(Dispatchers.IO) {
```

To:

```kotlin
suspend fun generateActivityDescription(
    activityTitle: String,
    sessionMode: String,
    items: List<String>,
    configurations: List<String>? = null,
    phonicsContext: String? = null
): String? = withContext(Dispatchers.IO) {
```

**Step 2: Add phonics context to the user prompt**

Replace the `configInfo` block (lines 332-335) with:

```kotlin
val configInfo = if (!isTutorial && !configurations.isNullOrEmpty()) {
    val uniqueConfigs = configurations.distinct()
    "\nActivity types used: ${uniqueConfigs.joinToString(", ")}."
} else ""

val phonicsInfo = if (!isTutorial && !phonicsContext.isNullOrBlank()) {
    "\nPhonics analysis:\n$phonicsContext"
} else ""
```

**Step 3: Update the system instruction prompt**

Replace the existing `systemInstruction` string (lines 337-359) with:

```kotlin
val systemInstruction = """
You are an educational content assistant. Generate a single factual sentence describing what students learn in this lesson and which specific items it covers.

The description should:
- State the learning objective (what skill students practice)
- Name the specific letters or words covered
- Mention the activity type if provided (air writing, fill-in-the-blanks, etc.)
- If phonics analysis is provided and shows a clear pattern (word family, shared vowel, same onset letter), mention the pattern naturally in the description
- If no clear phonics pattern exists, just describe the activity and words simply
- Be objective and informative, written for teachers
- Maximum 25 words

Examples:

Input: Tutorial, Uppercase Vowels, letters A E I O U
Output: Students practice forming and recognizing uppercase vowels A, E, I, O, U through guided writing.

Input: Tutorial, Lowercase Consonants, letters b c d f g h j k l m n p q r s t v w x y z
Output: Covers stroke-by-stroke practice of all 21 lowercase consonant letters.

Input: Learn, "-at Family", words cat bat hat, configurations: fill in the blanks, phonics: Word family: -at (cat, bat, hat); Fill-in-the-blanks targets: onset (first letter)
Output: Students identify missing onset consonants in -at family words cat, bat, and hat through fill-in-the-blanks.

Input: Learn, "Short O Words", words dog bob mob, configurations: fill in the blanks, phonics: Shared vowel sound: short 'o' (dog, bob, mob); Fill-in-the-blanks targets: medial vowel (middle letter)
Output: Students practice short-o vowel recognition in CVC words dog, bob, and mob using fill-in-the-blanks.

Input: Learn, "Mixed CVC", words cat dog pen, configurations: write the word, fill in the blanks
Output: Students practice reading and writing CVC words cat, dog, and pen using air writing and fill-in-the-blanks.

Respond with ONLY the description text, no JSON, no markdown, no quotes.
""".trimIndent()
```

**Step 4: Add phonics info to the user prompt**

Replace the existing `userPrompt` string (lines 361-367) with:

```kotlin
val userPrompt = """
Lesson: $activityTitle
Mode: ${if (isTutorial) "Tutorial (letter practice)" else "Learn (word practice)"}
${itemType.replaceFirstChar { it.uppercase() }} covered: ${items.joinToString(", ")}$configInfo$phonicsInfo

Generate one factual sentence describing this lesson.
""".trimIndent()
```

---

### Task 4: Update LearnAnnotationDetailsScreen to build and pass phonics context

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/classroom/LearnAnnotationDetailsScreen.kt:89-132`

**Step 1: Replace the description LaunchedEffect block**

Replace the entire `// Load or generate activity description` LaunchedEffect (lines 89-132) with:

```kotlin
// Load or generate activity description
LaunchedEffect(setId, activityId) {
    isLoadingDescription = true
    val descriptionDao = database.activityDescriptionCacheDao()

    // Clear stale Learn mode cache (one-time migration for phonics-aware descriptions)
    descriptionDao.deleteBySessionMode("LEARN")

    val cached = descriptionDao.getDescription(
        setId = setId,
        sessionMode = "LEARN",
        activityId = activityId
    )
    if (cached != null) {
        descriptionText = cached.descriptionText
        isLoadingDescription = false
    } else {
        val setWordDao = database.setWordDao()
        val wordDao = database.wordDao()
        val loadedSetWords = setWordDao.getSetWords(setId)
        val wordNames = loadedSetWords.mapNotNull { sw ->
            wordDao.getWordById(sw.wordId)?.word
        }
        val configs = loadedSetWords.map { it.configurationType }
        val letterIndices = loadedSetWords.map { it.selectedLetterIndex }

        if (wordNames.isNotEmpty()) {
            val geminiRepository = GeminiRepository()
            val phonicsContext = geminiRepository.buildPhonicsContext(
                wordNames = wordNames,
                selectedLetterIndices = letterIndices,
                configurations = configs
            )
            val generated = geminiRepository.generateActivityDescription(
                activityTitle = lessonName,
                sessionMode = "LEARN",
                items = wordNames,
                configurations = configs,
                phonicsContext = phonicsContext.ifBlank { null }
            )
            if (generated != null) {
                descriptionText = generated
                descriptionDao.insertOrUpdate(
                    ActivityDescriptionCache(
                        setId = setId,
                        sessionMode = "LEARN",
                        activityId = activityId,
                        descriptionText = generated
                    )
                )
            }
        }
        isLoadingDescription = false
    }
}
```

**Note on cache clearing:** The `deleteBySessionMode("LEARN")` call at the top of the LaunchedEffect ensures all old Learn mode descriptions are regenerated with phonics context. After the first run, subsequent visits will hit the newly cached descriptions. This is a simple one-time migration approach — the extra DELETE query on cached data is negligible.

---

### Task 5: Remove one-time cache clear after first deployment

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/classroom/LearnAnnotationDetailsScreen.kt`

**Note:** This is a follow-up task. After the app has been deployed and users have regenerated their descriptions, remove the `descriptionDao.deleteBySessionMode("LEARN")` line from the LaunchedEffect to stop clearing the cache on every screen load. Alternatively, gate it behind a SharedPreferences flag:

```kotlin
val prefs = context.getSharedPreferences("app_migrations", Context.MODE_PRIVATE)
if (!prefs.getBoolean("phonics_desc_v1_migrated", false)) {
    descriptionDao.deleteBySessionMode("LEARN")
    prefs.edit().putBoolean("phonics_desc_v1_migrated", true).apply()
}
```

This can be done in the same task as Task 4 if preferred — just wrap the delete call in the SharedPreferences check.

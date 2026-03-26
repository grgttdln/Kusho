# Predefined Starter Word Bank, Sets & Activity

## Overview

Every new teacher account receives a predefined starter word bank, two word sets, and one activity automatically upon signup. This gives teachers an immediate, usable example of how activities work without requiring manual setup.

All starter data is fully editable and deletable — treated identically to user-created content.

## Predefined Data

### Words (12 CVC words)

| Word | Image Asset | Used In |
|------|-------------|---------|
| cat  | `cat.png`   | Set 1   |
| dog  | `dog.png`   | Set 1   |
| sun  | `sun.png`   | Set 1   |
| cup  | -           | Set 1   |
| hat  | -           | Set 1   |
| bed  | -           | Set 1   |
| pig  | `pig.png`   | Set 2   |
| fox  | `fox.png`   | Set 2   |
| pen  | -           | Set 2   |
| map  | -           | Set 2   |
| bus  | -           | Set 2   |
| nut  | -           | Set 2   |

Words with images are used for "name the picture" questions. The user will provide the actual image files.

### Sets

**Set 1: "Starter: Simple CVC Words"** (6 words)

| Word | Configuration Type   | Notes                          |
|------|----------------------|--------------------------------|
| cat  | name the picture     | Uses `cat.png`                 |
| dog  | name the picture     | Uses `dog.png`                 |
| sun  | name the picture     | Uses `sun.png`                 |
| cup  | fill in the blanks   | `selectedLetterIndex` = 1 ("u")|
| hat  | fill in the blanks   | `selectedLetterIndex` = 1 ("a")|
| bed  | write the word       |                                |

**Set 2: "Starter: More CVC Words"** (6 words)

| Word | Configuration Type   | Notes                          |
|------|----------------------|--------------------------------|
| pig  | name the picture     | Uses `pig.png`                 |
| fox  | name the picture     | Uses `fox.png`                 |
| pen  | fill in the blanks   | `selectedLetterIndex` = 1 ("e")|
| map  | fill in the blanks   | `selectedLetterIndex` = 1 ("a")|
| bus  | write the word       |                                |
| nut  | write the word       |                                |

### Activity

- **Title:** "Starter: CVC Word Practice"
- **Description:** "Practice reading and writing simple CVC words"
- **Sets linked:** Both Set 1 and Set 2

## Architecture

### New File: `SeedRepository.kt`

**Location:** `app/src/main/java/com/example/app/data/repository/SeedRepository.kt`

**Responsibilities:**
- Define all starter data as private constants (word lists, set definitions, configuration mappings)
- Provide a single public method: `suspend fun seedDefaultData(context: Context, userId: Long)`
- Execute all insertions in a single Room `@Transaction`

**Seed flow (all within one transaction):**
1. Copy image assets from `assets/starter_images/` to internal storage (`filesDir/word_images/`)
2. Insert 12 `Word` entities for the user, storing `imagePath` for words with images -> collect generated word IDs
3. Insert 2 `Set` entities for the user -> collect set IDs
4. Insert `SetWord` junction records mapping words to sets with their configuration types, `selectedLetterIndex`, and `imagePath`
5. Insert 1 `Activity` entity for the user -> collect activity ID
6. Insert 2 `ActivitySet` junction records linking both sets to the activity

**Dependencies:** Uses existing `WordDao`, `SetDao`, `SetWordDao`, `ActivityDao` — no new DAOs needed.

### Image Assets

- Stored at: `app/src/main/assets/starter_images/`
- Files: `cat.png`, `dog.png`, `sun.png`, `pig.png`, `fox.png` (provided by user)
- On seed: copied to `context.filesDir/word_images/<filename>` so they behave like any user-added image
- The `imagePath` stored in `Word` and `SetWord` entities will be the internal storage path

### Singleton Pattern

`SeedRepository` follows the existing singleton pattern (`@Volatile` + `synchronized` double-check locking) with `getInstance(context)`, receiving DAOs from `AppDatabase.getInstance(context)`.

## Integration Point

### SignUpViewModel

**File:** `app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpViewModel.kt`

**Change:** After `UserRepository.signUp()` returns `SignUpResult.Success`, call `SeedRepository.seedDefaultData(context, result.userId)`.

```
SignUpResult.Success -> {
    // Existing: save session
    // New: seed starter data (non-fatal on failure)
    try {
        SeedRepository.getInstance(context).seedDefaultData(context, result.userId)
    } catch (e: Exception) {
        Log.e("SignUp", "Failed to seed starter data", e)
    }
    // Existing: navigate to onboarding
}
```

**Error handling:** Seeding failure is non-fatal. The user account is already created. A failed seed just means the teacher starts with an empty workspace (the current behavior). Error is logged but does not block signup or show an error to the user.

## Scope Boundaries

- No UI changes — starter data appears naturally in existing word bank, sets, and activities screens
- No new database entities or schema changes
- No changes to existing DAOs
- No special "starter" flag or read-only protection — data is fully mutable
- No seeding for existing users — only new signups

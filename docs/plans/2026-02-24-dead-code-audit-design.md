# Dead Code Audit — Kusho Project

**Date:** 2026-02-24
**Scope:** Safe removals only (definitively unused code)
**Status:** Documentation only — no code changes made

---

## Definitively Unused Code (Safe to Remove)

### 1. `PracticeMode()` composable + `recordSensorData()` helper

- **File:** `wear/src/main/java/com/example/kusho/presentation/screens/mode/PracticeMode.kt`
- **~175 lines** of unused composition and sensor code
- **Why unused:** The app uses `PracticeModeScreen()` instead. This appears to be a legacy/experimental version.
- **Verification:** Zero references to `PracticeMode()` across the entire codebase.

### 2. `WordValidator.getCVCExamples()`

- **File:** `app/src/main/java/com/example/app/util/WordValidator.kt` (lines ~89-95)
- **~7 lines** — returns a hardcoded list of example CVC words
- **Why unused:** Function is defined but never called anywhere.
- **Verification:** Zero references across the entire codebase.

### 3. `StudentSetProgressDao` (entire DAO)

- **File:** `app/src/main/java/com/example/app/data/dao/StudentSetProgressDao.kt`
- **Why unused:** DAO is declared in `AppDatabase.kt` but no repository or ViewModel ever calls any of its methods.
- **Verification:** Only appears in `AppDatabase.kt` abstract method declaration. Zero usage elsewhere.
- **Note:** The corresponding entity `StudentSetProgress` may also be removable if no other code references it.

### 4. `LearnerProfileAnnotationDao` (entire DAO)

- **File:** `app/src/main/java/com/example/app/data/dao/LearnerProfileAnnotationDao.kt`
- **Why unused:** DAO exists but is never instantiated or called outside of `AppDatabase.kt`.
- **Verification:** The entity `LearnerProfileAnnotation` may be used elsewhere, but the DAO itself has zero callers.

### 5. `EnrollmentRepository.updateProgress()` and `updateAnalytics()`

- **File:** `app/src/main/java/com/example/app/data/repository/EnrollmentRepository.kt` (lines ~148-180)
- **~20 lines** of unused repository methods
- **Why unused:** Defined but never called from any ViewModel or screen.
- **Verification:** Zero references to these method names across the codebase.

### 6. `DictionaryValidator.onGetSuggestions()` body

- **File:** `app/src/main/java/com/example/app/util/DictionaryValidator.kt` (lines ~95-97)
- **Why unused:** Explicitly marked with comment: `"Not used - we use getSentenceSuggestions instead"`
- **Note:** Required by the `SpellCheckerSession.SpellCheckerSessionListener` interface, so the method signature must stay. Only the body logic is dead.

---

## Items Investigated but Excluded (Not Safe to Remove)

These items have limited usage but ARE referenced somewhere, or need further investigation:

| Item | Reason for Exclusion |
|------|---------------------|
| `ActivityDescriptionCacheDao` | Used directly in `AnnotationDetailsScreen` (bypasses repository but IS used) |
| `TutorialCompletionDao` | Used directly in `TutorialSessionScreen` |
| `KuuRecommendationDao` | Used directly in `ClassroomViewModel` and `LearnModeSessionScreen` |
| `AnnotationSummaryDao` | Used through direct database calls |
| `StudentTeacherRepository` | Has 2 active usages in ViewModels |
| `ImageUtil` vs `ImageStorageManager` | Duplicate functionality but both are actively referenced — consolidation task, not a removal |
| `WordValidator.isThreeLetters()` | Called internally within `validateWordForBank()` — could be inlined but not dead |

---

## Other Observations (Not Dead Code, but Notable)

### TODO Stubs (Incomplete Features)
These are not dead code but placeholders for unimplemented error handling:

- `ClassDetailsScreen.kt` — `// TODO: Show error toast/snackbar`
- `EditClassScreen.kt` — `// TODO: Show error toast/snackbar`
- `StudentDetailsScreen.kt` — `// TODO: Show error toast/snackbar`
- `ClassScreen.kt` — `// TODO: show snackbar/toast`
- `DashboardScreen.kt` — `// TODO: Dashboard sections go here`

### Debug Bypasses (Wear Module)
Motion gate temporarily bypassed in 3 ViewModels:
- `TutorialModeViewModel`
- `LearnModeViewModel`
- `PracticeModeViewModel`

### Architecture Inconsistencies
Several DAOs are accessed directly from UI screens instead of through the repository pattern. This isn't dead code but deviates from the project's stated architecture.

---

## Estimated Impact

| Category | Items | Lines |
|----------|-------|-------|
| Unused composables | 1 file | ~175 |
| Unused functions | 2 | ~10 |
| Unused DAOs | 2 files | ~50 |
| Unused repository methods | 2 | ~20 |
| Dead interface body | 1 | ~3 |
| **Total** | **8 items** | **~258 lines** |

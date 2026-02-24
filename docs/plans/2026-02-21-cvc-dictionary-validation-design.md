# CVC Dictionary Validation Design

## Problem

The WordBank currently validates CVC pattern (consonant-vowel-consonant) but does not verify that the word is a real English word. Users can add nonsense like "bof" or "zup" as long as it matches the CVC pattern.

## Solution

Use Android's built-in `SpellCheckerSession` API to validate words against the system dictionary. Block invalid words and suggest similar real CVC words.

## Architecture

### New Component: `DictionaryValidator`

**Location:** `app/src/main/java/com/example/app/util/DictionaryValidator.kt`

- Wraps `android.view.textservice.SpellCheckerSession`
- Exposes `suspend fun validateWord(word: String): DictionaryResult`
- `DictionaryResult` is a sealed class: `Valid` or `Invalid(suggestions: List<String>)`
- Bridges callback API to coroutines via `suspendCancellableCoroutine`
- Implements `Closeable` for session lifecycle management
- Suggestions filtered to CVC-pattern words only via `WordValidator.isCVCPattern()`

### Validation Flow

**Current:** `WordValidator.validateWordForBank()` -> duplicate check -> insert

**New:** `WordValidator.validateWordForBank()` -> `DictionaryValidator.validateWord()` -> duplicate check -> insert

Dictionary check only runs after CVC validation passes.

### Repository Changes

- `WordRepository` receives `DictionaryValidator` via injection
- `addWord()` and `updateWord()` gain dictionary validation step
- New result variant: `AddWordResult.NotInDictionary(suggestions: List<String>)`

### ViewModel Changes

- `LessonUiState` gains `dictionarySuggestions: List<String>` field
- `LessonViewModel` handles `NotInDictionary` result by populating suggestions and setting error text
- Suggestions cleared when user starts typing again

### UI Changes (WordBankModal + WordBankEditModal)

- When `dictionarySuggestions` is non-empty, show clickable suggestion chips below the error message
- Tapping a suggestion fills the word input field
- Error message: "This word was not found in the dictionary"

## Error Handling

- **No spell checker service:** Accept the word, log warning
- **Timeout (3s):** Accept the word (don't block users)
- **Empty suggestions:** Show generic error without chips
- **Non-CVC suggestions:** Filtered out, only CVC suggestions shown

## Scope

- Applies to both add and edit flows
- AI-generated words (WordBankGenerationModal) go through same `addWord()` path, automatically validated

## Technical Notes

- `SpellCheckerSession` works offline (dictionary is stored locally on device)
- Dictionary data comes from the spell checker service pre-installed on the device
- No internet connection required for word lookup

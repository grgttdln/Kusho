# CLAUDE.md — Kusho Project

## Project Overview

Kusho is an educational Android app for letter and word learning, with Wear OS smartwatch integration, AI-powered activity generation, and air-writing gesture recognition via TensorFlow Lite.

## Modules

- **:app** — Main phone application (Kotlin, Jetpack Compose, Material 3)
- **:wear** — Wear OS smartwatch app (TFLite gesture recognition, sensor input)
- **:common** — Shared utilities for phone-watch communication

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumentation tests
```

- Min SDK: 24 (app), 30 (wear)
- Compile/Target SDK: 36
- JVM Target: Java 11
- Gradle: 8.13.2, Kotlin: 2.0.21

## API Keys

Stored in `local.properties` (not committed), injected via `BuildConfig`:

- `DEEPGRAM_API_KEY` — Text-to-speech (Deepgram)
- `GEMINI_API_KEY` — AI activity generation (OpenRouter)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (100%) |
| UI | Jetpack Compose + Material 3 (no XML) |
| Architecture | MVVM with Repository pattern |
| Database | Room (v2.6.1), database version 18 |
| Navigation | Navigation Compose with sealed class routes |
| Networking | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson |
| AI | Google Generative AI SDK 0.9.0, OpenRouter API |
| TTS | Deepgram API (primary), Android TTS (fallback) |
| ML | TensorFlow Lite 2.17.0 (wear module, air-writing) |
| Watch Comms | Play Services Wearable 19.0.0 |
| Images | Coil Compose 2.5.0 |
| DI | Manual singletons (no Hilt/Dagger) |
| State | StateFlow / MutableStateFlow in ViewModels |
| Testing | JUnit 4, Espresso, Compose UI Test |
| Dependencies | Version catalog (`gradle/libs.versions.toml`) |

## Architecture

```
app/src/main/java/com/example/app/
├── data/
│   ├── AppDatabase.kt          # Room database (16 entities, 14 DAOs)
│   ├── SessionManager.kt       # User session singleton
│   ├── dao/                    # Room DAOs
│   ├── entity/                 # Room entities
│   ├── model/                  # API response models
│   └── repository/             # 9 repositories (business logic)
├── ui/
│   ├── feature/                # Screen-level composables + ViewModels
│   │   ├── auth/               # Login, SignUp
│   │   ├── classroom/          # Class management
│   │   ├── dashboard/          # Main dashboard
│   │   ├── home/               # MainNavigationContainer
│   │   ├── learn/              # Learning flows, AI generation, sets
│   │   ├── onboarding/         # Onboarding screens
│   │   └── watch/              # Watch pairing
│   ├── components/             # Reusable composable components
│   ├── theme/                  # Color, Typography, Theme
│   └── navigation/             # AppNavigation, Screen sealed class
├── service/                    # Background services
├── speech/                     # TTS integration
└── util/                       # Utilities
```

## Code Conventions

- **Kotlin only** — no Java
- **Compose only** — no XML layouts
- **Screens** end with `Screen` suffix (e.g., `LearnModeScreen`)
- **ViewModels** extend `AndroidViewModel`, live alongside their feature screen
- **State**: expose `StateFlow`, mutate via `MutableStateFlow` + `_uiState.update {}`
- **Repositories** abstract all data access; return sealed class results for operations
- **Singletons** use `@Volatile` + `synchronized` double-check locking with `getInstance()`
- **Naming**: PascalCase classes, camelCase functions, UPPER_SNAKE_CASE constants
- **One public class per file**
- **Coroutines** via `viewModelScope` in ViewModels, `LaunchedEffect` in Compose
- **Room** uses `fallbackToDestructiveMigration()` — no manual migrations

## Key Features

1. **Auth** — Login/signup with password hashing + salt
2. **Dashboard** — Activity progress, student cards, Kuu recommendations, watch status
3. **Classroom Management** — Create/edit/archive classes, manage students
4. **Learn Mode** — Tutorial mode (guided) and Learn mode (independent), fill-in-blank / write-word / name-picture question types
5. **Word Bank** — Per-user word management with optional images, batch delete
6. **Activities & Sets** — Create activities, build word sets, link them together
7. **AI Activity Generation** — Natural language prompts generate activities via Gemini 2.5 Flash Lite with PatternRouter for deterministic CVC pattern filtering
8. **Watch Integration** — Air-writing gesture recognition, tutorial/learn modes on watch, phone-watch sync
9. **TTS** — Deepgram voices with audio caching, playback speed control, encouraging phrases

## Navigation

Routes defined in `Screen` sealed class. Main navigation handled by `MainNavigationContainer` with 50+ screen indices. Key routes:

- `Login`, `SignUp`, `PostSignUpOnboarding`
- `Home` (gateway to all features)
- `ClassDetails/{classId}`, `StudentDetails/{studentId}/{studentName}/{className}`

## Database

Room database `kusho_database` (version 18) with 16 entities including: `User`, `Word`, `Class`, `Student`, `Enrollment`, `Activity`, `Set`, `SetWord`, `StudentSetProgress`, `TutorialCompletion`, and more. CASCADE DELETE on user deletion.

## AI Activity Generation Pipeline

The activity generation system in `GeminiRepository.kt` uses a 3-step chained Gemini pipeline:

1. **Step 1 — Filter Words**: Selects words from the Word Bank matching the teacher's prompt
2. **Step 2 — Select Subset**: Picks a coherent group of 3-10 words
3. **Step 3 — Configure**: Assigns activity types ("write the word", "fill in the blanks", "name the picture")

### PatternRouter (deterministic filtering)

`classifyPrompt()` detects structural CVC patterns in teacher prompts and bypasses AI for Step 1:

| Pattern Type | Example Prompt | Detection |
|---|---|---|
| Rime | "-un words", "ending in -at" | `DetectedPattern.Rime` |
| Onset | "starting with b", "letters with g" | `DetectedPattern.Onset` |
| Vowel | "short a words", "vowel 'e'" | `DetectedPattern.Vowel` |
| Coda | "ending in t" | `DetectedPattern.Coda` |

Regex ordering matters: **vowel patterns are checked before onset** to prevent "words with the short 'a'" from matching 's' in "short" as an onset. The `(?:the\s+)?(?:letter\s+)?` optional groups handle phrasings like "start with the letter g".

Semantic/thematic prompts (e.g., "animal words") fall through to AI Step 1.

### Insufficient Word Bank → Inline Suggestions

When fewer than 3 words match a structural pattern:
- `generateActivity()` returns `AiGenerationResult.InsufficientWords` (carries pattern + original prompt)
- `LessonViewModel.handleInsufficientWords()` generates suggestions via `generateCVCWords()` using the original teacher prompt
- `WordSuggestionDialog` shows selectable word chips for the teacher to add to the bank
- After adding, generation auto-resumes

### Word Generation Filtering

`generateCVCWords()` applies a post-generation filter chain:
1. `isValidCVC()` — validates consonant-vowel-consonant pattern
2. `BLOCKED_WORDS` — removes inappropriate content
3. **Pattern adherence** — if `classifyPrompt()` detects a structural pattern, non-matching words are stripped
4. Child-friendly vocabulary enforcement via prompt (kindergarten/first-grade level, explicit negative examples)

### Key Files

| File | Role |
|---|---|
| `data/repository/GeminiRepository.kt` | AI pipeline, PatternRouter, CVC analysis |
| `data/model/AiGeneratedData.kt` | Result types including `InsufficientWords` |
| `ui/feature/learn/LessonViewModel.kt` | Word bank management, suggestion flow |
| `ui/components/wordbank/WordSuggestionDialog.kt` | Inline suggestion dialog |
| `ui/feature/learn/set/YourSetsScreen.kt` | Activity creation UI, dialog wiring |
| `util/WordValidator.kt` | CVC pattern validation |
| `util/DictionaryValidator.kt` | Android spell checker integration |

## Permissions

Bluetooth, Internet, Notifications (Android 13+), Vibrate

## Entry Points

- `MainActivity.kt` — Single activity, sets up Compose
- `AppNavigation.kt` — Top-level navigation graph
- `MainNavigationContainer.kt` — Feature routing

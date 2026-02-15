# Architecture

**Analysis Date:** 2026-02-15

## Pattern Overview

**Overall:** Multi-module Android application with MVVM + Repository pattern

**Key Characteristics:**
- Three-module structure: mobile app (`:app`), wearable app (`:wear`), shared common (`:common`)
- Mobile-to-wearable communication via Google Wearable MessageClient
- Local-first data persistence using Room Database
- Jetpack Compose UI with declarative navigation
- Machine learning inference on wearable device using TensorFlow Lite

## Layers

**Presentation Layer:**
- Purpose: UI rendering and user interaction handling
- Location: `app/src/main/java/com/example/app/ui/`
- Contains: Composable screens, ViewModels, UI components
- Depends on: Data layer (repositories, entities), Navigation
- Used by: MainActivity

**Data Layer:**
- Purpose: Data persistence, business logic, and data access abstraction
- Location: `app/src/main/java/com/example/app/data/`
- Contains: Room Database, DAOs, Repositories, Entities
- Depends on: Room persistence library, Kotlin Coroutines
- Used by: ViewModels in presentation layer

**Navigation Layer:**
- Purpose: Screen routing and navigation state management
- Location: `app/src/main/java/com/example/app/navigation/`
- Contains: Navigation graph, Screen definitions, route management
- Depends on: Jetpack Navigation Compose
- Used by: MainActivity, feature screens

**Service Layer:**
- Purpose: Cross-cutting concerns and background operations
- Location: `app/src/main/java/com/example/app/service/`, `wear/src/main/java/com/example/kusho/presentation/service/`
- Contains: Watch connection management, phone communication, message listeners
- Depends on: Google Wearable API
- Used by: MainActivity, ViewModels

**ML Inference Layer (Wear):**
- Purpose: Real-time gesture recognition using on-device machine learning
- Location: `wear/src/main/java/com/example/kusho/ml/`
- Contains: TensorFlow Lite classifiers, model loaders, sensor processing
- Depends on: TensorFlow Lite runtime, sensor data
- Used by: Practice/Tutorial/Learn mode screens on wearable

**Common Module:**
- Purpose: Shared code between mobile and wearable apps
- Location: `common/src/main/java/com/example/kusho/common/`
- Contains: MessageService for bidirectional communication
- Depends on: Google Wearable API
- Used by: Both `:app` and `:wear` modules

## Data Flow

**Authentication Flow:**

1. User enters credentials in `LoginScreen`
2. `LoginViewModel` validates input and calls `UserRepository.login()`
3. `UserRepository` queries `UserDao` for user by email
4. Password verification using `PasswordUtils.verifyPassword()` with stored hash and salt
5. On success, `SessionManager.saveUserSession()` persists session to SharedPreferences
6. Navigation to Home or WatchPairing screen based on connection status

**Learn Mode Session Flow:**

1. Mobile app: Teacher selects student and activity in `LearnModeActivitySelectionScreen`
2. Mobile app: Teacher selects set in `LearnModeSetStatusScreen`
3. Mobile app: `LearnModeSessionScreen` ViewModel loads words from `SetRepository`
4. Mobile app: Sends word to wearable via `MessageService`
5. Wearable: Displays word in `LearnModeScreen`
6. Wearable: Student performs air-writing gesture
7. Wearable: `AirWritingClassifier` processes sensor data and predicts character
8. Wearable: Sends prediction result back to mobile via `PhoneCommunicationManager`
9. Mobile app: Validates answer, provides feedback via Deepgram TTS
10. Mobile app: Records result in `LearnerProfileAnnotation` entity
11. Mobile app: Updates `StudentSetProgress` on completion

**Database Operations Flow:**

1. ViewModel calls Repository method (e.g., `UserRepository.signUp()`)
2. Repository performs business logic validation
3. Repository executes DAO operation on IO dispatcher using `withContext(Dispatchers.IO)`
4. DAO queries Room Database using SQL
5. Repository returns sealed class result (`Success` or `Error`)
6. ViewModel updates StateFlow with result
7. UI observes StateFlow and recomposes

**State Management:**
- ViewModels expose `StateFlow<UiState>` for reactive UI updates
- Repositories use suspend functions with Dispatchers.IO for database operations
- SessionManager provides singleton StateFlow for current user
- Navigation state managed by integer screen index in `MainNavigationContainer`

## Key Abstractions

**Repository Pattern:**
- Purpose: Single source of truth for data operations, abstracts data sources
- Examples: `app/src/main/java/com/example/app/data/repository/UserRepository.kt`, `app/src/main/java/com/example/app/data/repository/WordRepository.kt`, `app/src/main/java/com/example/app/data/repository/ActivityRepository.kt`
- Pattern: Interface to DAO, sealed class results, suspend functions with IO dispatcher

**Entity-DAO Pattern:**
- Purpose: Type-safe database operations with compile-time SQL verification
- Examples: `app/src/main/java/com/example/app/data/entity/User.kt` + `app/src/main/java/com/example/app/data/dao/UserDao.kt`
- Pattern: Room Entity annotations, DAO interface with suspend functions, Flow for reactive queries

**Singleton Managers:**
- Purpose: Single instance for app-wide state and services
- Examples: `app/src/main/java/com/example/app/data/SessionManager.kt`, `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`, `wear/src/main/java/com/example/kusho/presentation/service/ConnectionMonitor.kt`
- Pattern: Companion object with `@Volatile INSTANCE`, double-checked locking, context-aware initialization

**Sealed Navigation Routes:**
- Purpose: Type-safe navigation with compile-time route verification
- Examples: `app/src/main/java/com/example/app/navigation/Screen.kt`, `wear/src/main/java/com/example/kusho/presentation/navigation/NavigationRoutes.kt`
- Pattern: Sealed class with object instances, parameterized routes with `createRoute()` methods

**ML Classifier Interface:**
- Purpose: Pluggable machine learning model implementations
- Examples: `wear/src/main/java/com/example/kusho/ml/AirWritingClassifier.kt`, `wear/src/main/java/com/example/kusho/ml/SimpleTFLiteClassifier.kt`
- Pattern: Interface with `classify()` method, `PredictionResult` data class, resource cleanup via `close()`

## Entry Points

**Mobile App Entry:**
- Location: `app/src/main/java/com/example/app/MainActivity.kt`
- Triggers: Android launcher intent (MAIN/LAUNCHER)
- Responsibilities: Initialize MessageService, set up Compose UI with AppNavigation, enable edge-to-edge display

**Wearable App Entry:**
- Location: `wear/src/main/java/com/example/kusho/presentation/MainActivity.kt`
- Triggers: Wear OS launcher
- Responsibilities: Initialize PhoneCommunicationManager, start battery monitoring, display splash screen, navigate to pairing or home

**Database Entry:**
- Location: `app/src/main/java/com/example/app/data/AppDatabase.kt`
- Triggers: First call to `AppDatabase.getInstance(context)`
- Responsibilities: Create/migrate Room database, provide DAO instances, manage database lifecycle

**Navigation Entry:**
- Location: `app/src/main/java/com/example/app/navigation/AppNavigation.kt`
- Triggers: Composed by MainActivity
- Responsibilities: Determine start destination based on session state, set up NavHost with auth and main flows

**Main Navigation Container:**
- Location: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt`
- Triggers: After successful authentication
- Responsibilities: Manage 47 screen states with manual navigation via integer indices, coordinate classroom/learn/dashboard flows

## Error Handling

**Strategy:** Sealed class results with explicit error types

**Patterns:**
- Repository methods return sealed classes: `SignUpResult.Success` or `SignUpResult.Error(message)`
- ViewModels catch errors in coroutines and update `UiState.errorMessage`
- UI displays error dialogs or inline error messages based on StateFlow
- Database errors wrapped with user-friendly messages: `"Failed to create account: ${e.message}"`
- ML inference errors return `PredictionResult.error(message)` instead of throwing exceptions
- Wearable connection failures tracked by `ConnectionMonitor` with retry logic

## Cross-Cutting Concerns

**Logging:** Minimal structured logging; primarily uses `printStackTrace()` for exceptions in catch blocks

**Validation:**
- Input validation in ViewModels before repository calls (e.g., `email.isBlank()` checks)
- Business validation in Repositories (e.g., `PasswordUtils.validatePasswordStrength()`)
- Database constraints via Room annotations (e.g., unique email index)

**Authentication:**
- PBKDF2WithHmacSHA256 password hashing with per-user salt
- Session persistence via `SessionManager` using SharedPreferences
- Session check in `AppNavigation` determines start destination
- Logout clears session and navigates to onboarding

**Security:**
- Passwords stored as salted hashes, never plain text
- Room database encrypted via standard Android SQLite (no additional encryption)
- Wearable communication via Google Wearable API (encrypted channel)
- No external API authentication detected (Deepgram API key likely in code or environment)

**Threading:**
- All database operations on `Dispatchers.IO` via `withContext()`
- UI state updates on main thread via `StateFlow`
- Coroutines launched in `viewModelScope` for automatic cancellation
- Sensor data collection and ML inference on background threads

---

*Architecture analysis: 2026-02-15*

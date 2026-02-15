# Codebase Structure

**Analysis Date:** 2026-02-15

## Directory Layout

```
Kusho/
├── app/                                # Mobile Android app module
│   ├── src/main/
│   │   ├── java/com/example/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/                   # Data layer (Room, DAOs, Repositories)
│   │   │   ├── navigation/             # Navigation graph and routes
│   │   │   ├── service/                # Background services (watch connection)
│   │   │   ├── speech/                 # TTS managers (Deepgram, Android TTS)
│   │   │   ├── ui/                     # Presentation layer (Compose UI)
│   │   │   └── util/                   # Utilities (image, password, validation)
│   │   ├── res/                        # Android resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── wear/                               # Wearable Android app module
│   ├── src/main/
│   │   ├── java/com/example/kusho/
│   │   │   ├── ml/                     # Machine learning (TFLite classifiers)
│   │   │   ├── presentation/           # UI and services
│   │   │   ├── sensors/                # Sensor data collection
│   │   │   ├── speech/                 # Wearable TTS
│   │   │   └── wordformation/          # Word processing logic
│   │   ├── assets/                     # TensorFlow Lite models (5 .tflite files)
│   │   ├── res/                        # Wear OS resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── common/                             # Shared code module
│   ├── src/main/java/com/example/kusho/common/
│   │   └── MessageService.kt           # Wearable communication service
│   └── build.gradle.kts
├── .planning/                          # GSD planning documents
│   └── codebase/
├── docs/                               # Project documentation
├── gradle/                             # Gradle wrapper
├── build.gradle.kts                    # Root build config
├── settings.gradle.kts                 # Module configuration
└── DEEPGRAM_SETUP.md                   # Integration documentation
```

## Directory Purposes

**app/src/main/java/com/example/app/data/**
- Purpose: Data persistence and business logic layer
- Contains: Room Database, DAOs (11 files), Entities (11 files), Repositories (7 files), SessionManager
- Key files: `AppDatabase.kt` (database singleton), `SessionManager.kt` (auth session)

**app/src/main/java/com/example/app/ui/feature/**
- Purpose: Feature-based screen organization
- Contains: auth/, classroom/, dashboard/, home/, learn/, onboarding/, watch/ subdirectories
- Key files: `auth/login/LoginScreen.kt`, `dashboard/DashboardScreen.kt`, `home/MainNavigationContainer.kt`

**app/src/main/java/com/example/app/ui/components/**
- Purpose: Reusable UI components organized by feature
- Contains: activities/, classroom/, dashboard/, learnmode/, wordbank/, common/ subdirectories
- Key files: `BottomNavBar.kt`, `PrimaryButton.kt`, `DeleteConfirmationDialog.kt`

**app/src/main/java/com/example/app/navigation/**
- Purpose: Navigation configuration and routing
- Contains: Navigation graph logic, sealed route classes
- Key files: `AppNavigation.kt` (NavHost setup), `Screen.kt` (route definitions)

**app/src/main/java/com/example/app/service/**
- Purpose: Background services and managers
- Contains: Watch connection management
- Key files: `WatchConnectionManager.kt`

**app/src/main/java/com/example/app/speech/**
- Purpose: Text-to-speech functionality
- Contains: Deepgram API integration, Android TTS wrapper
- Key files: `DeepgramTTSManager.kt`, `TextToSpeechManager.kt`

**app/src/main/java/com/example/app/util/**
- Purpose: Utility functions and helpers
- Contains: Image processing, password hashing, validation
- Key files: `PasswordUtils.kt`, `ImageStorageManager.kt`, `WordValidator.kt`

**wear/src/main/java/com/example/kusho/ml/**
- Purpose: Machine learning inference
- Contains: TensorFlow Lite classifiers, model configuration
- Key files: `AirWritingClassifier.kt` (interface), `SimpleTFLiteClassifier.kt` (implementation), `ModelLoader.kt`, `Labels.kt`

**wear/src/main/java/com/example/kusho/presentation/**
- Purpose: Wearable UI and services
- Contains: screens/, components/, navigation/, service/, theme/ subdirectories
- Key files: `MainActivity.kt`, `navigation/AppNavigation.kt`, `service/PhoneCommunicationManager.kt`

**wear/src/main/java/com/example/kusho/sensors/**
- Purpose: Sensor data collection and processing
- Contains: Accelerometer/gyroscope data handling
- Key files: Sensor collection logic for ML input

**wear/src/main/assets/**
- Purpose: Machine learning models
- Contains: 5 TensorFlow Lite models (complete_model_1.tflite, model2.tflite, tcn_multihead variants)

**common/src/main/java/com/example/kusho/common/**
- Purpose: Code shared between mobile and wearable
- Contains: Wearable MessageClient wrapper
- Key files: `MessageService.kt`

**app/src/main/res/drawable/**
- Purpose: Vector drawables and image assets
- Contains: 147 drawable files (icons, illustrations, banners)

**app/src/main/res/raw/**
- Purpose: Raw audio files
- Contains: Sound effects for feedback (correct/wrong sounds)

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/example/app/MainActivity.kt`: Mobile app entry point
- `wear/src/main/java/com/example/kusho/presentation/MainActivity.kt`: Wearable app entry point
- `app/src/main/java/com/example/app/navigation/AppNavigation.kt`: Mobile navigation entry
- `wear/src/main/java/com/example/kusho/presentation/navigation/AppNavigation.kt`: Wearable navigation entry

**Configuration:**
- `app/build.gradle.kts`: Mobile app dependencies and build config
- `wear/build.gradle.kts`: Wearable app dependencies and build config
- `common/build.gradle.kts`: Shared module config
- `settings.gradle.kts`: Multi-module project structure
- `build.gradle.kts`: Root project configuration
- `app/src/main/AndroidManifest.xml`: Mobile app permissions and components
- `wear/src/main/AndroidManifest.xml`: Wearable app permissions and components

**Core Logic:**
- `app/src/main/java/com/example/app/data/AppDatabase.kt`: Room database with 11 entities
- `app/src/main/java/com/example/app/data/SessionManager.kt`: Authentication session management
- `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt`: Main app navigation state machine (47 screens)
- `wear/src/main/java/com/example/kusho/ml/AirWritingClassifier.kt`: ML inference interface
- `common/src/main/java/com/example/kusho/common/MessageService.kt`: Mobile-wearable communication

**Testing:**
- `app/src/test/java/com/example/app/ExampleUnitTest.kt`: Unit test placeholder
- `app/src/androidTest/java/com/example/app/ExampleInstrumentedTest.kt`: Instrumented test placeholder

## Naming Conventions

**Files:**
- Screens: `{Feature}Screen.kt` (e.g., `LoginScreen.kt`, `DashboardScreen.kt`)
- ViewModels: `{Feature}ViewModel.kt` (e.g., `LoginViewModel.kt`, `DashboardViewModel.kt`)
- Components: `{Purpose}Component.kt` or `{Noun}.kt` (e.g., `PrimaryButton.kt`, `ClassCard.kt`)
- Repositories: `{Entity}Repository.kt` (e.g., `UserRepository.kt`, `WordRepository.kt`)
- DAOs: `{Entity}Dao.kt` (e.g., `UserDao.kt`, `ClassDao.kt`)
- Entities: `{EntityName}.kt` (e.g., `User.kt`, `Word.kt`, `Class.kt`)

**Directories:**
- Feature packages: lowercase, single word or camelCase (e.g., `auth/`, `classroom/`, `learnmode/`)
- Subdirectories by function: `login/`, `signup/` under `auth/`

**Classes:**
- Activities: `{Feature}Activity` (only `MainActivity` present)
- Screens: `{Feature}Screen` composable functions
- ViewModels: `{Feature}ViewModel` extending `AndroidViewModel`
- Repositories: `{Entity}Repository` class
- DAOs: `{Entity}Dao` interface
- Entities: `{EntityName}` data class with `@Entity` annotation

**Functions:**
- Composables: PascalCase (e.g., `LoginScreen()`, `PrimaryButton()`)
- Regular functions: camelCase (e.g., `saveUserSession()`, `getUserById()`)
- Suspend functions: camelCase with `suspend` modifier

**Variables:**
- camelCase for local and member variables
- SCREAMING_SNAKE_CASE for constants in companion objects

## Where to Add New Code

**New Feature Screen:**
- Primary code: `app/src/main/java/com/example/app/ui/feature/{featurename}/{FeatureName}Screen.kt`
- ViewModel: `app/src/main/java/com/example/app/ui/feature/{featurename}/{FeatureName}ViewModel.kt`
- Navigation: Add route to `app/src/main/java/com/example/app/navigation/Screen.kt` and composable to `AppNavigation.kt`
- Tests: `app/src/test/java/com/example/app/ui/feature/{featurename}/{FeatureName}ViewModelTest.kt`

**New Reusable Component:**
- General component: `app/src/main/java/com/example/app/ui/components/{ComponentName}.kt`
- Feature-specific component: `app/src/main/java/com/example/app/ui/components/{feature}/{ComponentName}.kt`

**New Data Entity:**
- Entity: `app/src/main/java/com/example/app/data/entity/{EntityName}.kt`
- DAO: `app/src/main/java/com/example/app/data/dao/{EntityName}Dao.kt`
- Repository: `app/src/main/java/com/example/app/data/repository/{EntityName}Repository.kt`
- Add entity to `AppDatabase.kt` entities array and create DAO getter method
- Increment database version number in `@Database` annotation

**New Wearable Screen:**
- Screen: `wear/src/main/java/com/example/kusho/presentation/screens/{featurename}/{FeatureName}Screen.kt`
- ViewModel (if needed): `wear/src/main/java/com/example/kusho/presentation/{feature}/{FeatureName}ViewModel.kt`
- Navigation: Add route to `wear/src/main/java/com/example/kusho/presentation/navigation/NavigationRoutes.kt` and composable to `AppNavigation.kt`

**New ML Model:**
- Model file: `wear/src/main/assets/{model_name}.tflite`
- Classifier implementation: `wear/src/main/java/com/example/kusho/ml/{ModelName}Classifier.kt` implementing `AirWritingClassifier` interface
- Model configuration: Update `wear/src/main/java/com/example/kusho/ml/ModelConfig.kt`

**Utilities:**
- Shared helpers: `app/src/main/java/com/example/app/util/{UtilityName}Utils.kt` or `{UtilityName}Manager.kt`
- Wearable utilities: `wear/src/main/java/com/example/kusho/presentation/utils/{UtilityName}.kt`

**New Service:**
- Mobile service: `app/src/main/java/com/example/app/service/{ServiceName}Manager.kt`
- Wearable service: `wear/src/main/java/com/example/kusho/presentation/service/{ServiceName}.kt`
- Register in respective `AndroidManifest.xml` if it's an Android Service component

**Resources:**
- Drawables: `app/src/main/res/drawable/{resource_name}.xml`
- Audio files: `app/src/main/res/raw/{sound_name}.mp3` or `.wav`
- Strings: `app/src/main/res/values/strings.xml`
- Theme colors: `app/src/main/java/com/example/app/ui/theme/Color.kt`

## Special Directories

**.gradle/**
- Purpose: Gradle build cache and temporary files
- Generated: Yes
- Committed: No

**app/build/** and **wear/build/**
- Purpose: Compiled outputs, intermediate build files
- Generated: Yes (by Gradle during build)
- Committed: No

**common/build/**
- Purpose: Compiled outputs for shared module
- Generated: Yes
- Committed: No

**.planning/**
- Purpose: GSD planning and codebase analysis documents
- Generated: No (manually created by GSD commands)
- Committed: Yes

**.opencode/**
- Purpose: OpenCode tooling artifacts
- Generated: Yes
- Committed: No

**docs/**
- Purpose: User-facing documentation
- Generated: No
- Committed: Yes

**.idea/**
- Purpose: IntelliJ IDEA/Android Studio project settings
- Generated: Yes
- Committed: Partial (gitignored with exceptions for shared settings)

**.kotlin/**
- Purpose: Kotlin compiler cache and session data
- Generated: Yes
- Committed: No

---

*Structure analysis: 2026-02-15*

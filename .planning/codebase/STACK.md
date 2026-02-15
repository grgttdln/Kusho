# Technology Stack

**Analysis Date:** 2026-02-15

## Languages

**Primary:**
- Kotlin 2.0.21 - Android application code (app and wear modules)
- Kotlin DSL - Gradle build scripts

**Secondary:**
- XML - Android manifest and resource files

## Runtime

**Environment:**
- Android SDK 36 (compileSdk and targetSdk for app module)
- Android SDK 30+ (minSdk for wear module)
- Android SDK 24+ (minSdk for app and common modules)

**Package Manager:**
- Gradle 8.13.2 with Kotlin DSL
- Lockfile: Not present (Gradle uses version catalogs)
- Version catalog: `gradle/libs.versions.toml`

**JVM:**
- Java 11 (sourceCompatibility and targetCompatibility)
- Kotlin JVM target: 11

## Frameworks

**Core:**
- Jetpack Compose (BOM 2024.09.00) - UI framework for both phone and wear
- Wear Compose 1.2.1 - Wear OS specific UI components
- AndroidX Core KTX 1.17.0 - Kotlin extensions for Android
- AndroidX Activity Compose 1.12.0 - Compose integration with Activity

**Testing:**
- JUnit 4.13.2 - Unit testing
- AndroidX Test JUnit 1.3.0 - Android instrumentation tests
- Espresso Core 3.7.0 - UI testing
- Compose UI Test JUnit4 - Compose UI testing

**Build/Dev:**
- Android Gradle Plugin (AGP) 8.13.2
- Kotlin Compiler Plugin for Compose 2.0.21
- KSP (Kotlin Symbol Processing) 2.0.21-1.0.27 - Annotation processing for Room

## Key Dependencies

**Critical:**
- Room 2.6.1 - Local SQLite database with type-safe DAO pattern
  - Location: `app/src/main/java/com/example/app/data/AppDatabase.kt`
  - Database name: "kusho_database"
  - Entities: User, Word, Student, Class, Enrollment, Activity, Set, SetWord, ActivitySet, StudentTeacher, LearnerProfileAnnotation, StudentSetProgress

- Google Play Services Wearable 19.0.0 - Communication between phone and Wear OS watch
  - DataClient, MessageClient, NodeClient, CapabilityClient
  - Location: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`

- TensorFlow Lite 2.17.0 - On-device ML inference for handwriting recognition
  - TensorFlow Lite Select TF Ops 2.16.1 - Additional ops support
  - Models location: `wear/src/main/assets/*.tflite`
  - ML classifier: `wear/src/main/java/com/example/kusho/ml/AirWritingClassifier.kt`

**Infrastructure:**
- Retrofit 2.9.0 - HTTP client for REST APIs
- Gson Converter 2.9.0 - JSON serialization/deserialization
- OkHttp 4.12.0 - HTTP client underlying Retrofit
- OkHttp Logging Interceptor 4.12.0 - Request/response logging

**UI/UX:**
- Material3 - Material Design 3 components (phone app)
- Material Icons Extended - Additional Material Design icons
- Navigation Compose 2.7.6 - In-app navigation
- Wear Navigation Compose 1.3.0 - Wear OS navigation
- Coil Compose 2.5.0 - Image loading and caching

**State Management:**
- Lifecycle ViewModel Compose 2.6.1 - UI state management
- Lifecycle Runtime KTX 2.6.1 - Lifecycle-aware components
- DataStore Preferences 1.0.0 - Key-value storage for session management

**Concurrency:**
- Kotlinx Coroutines Core 1.10.2 - Asynchronous programming
- Kotlinx Coroutines Play Services 1.10.2 - Coroutines for Google Play Services

**Wear OS Specific:**
- Wear Remote Interactions 1.0.0 - Opening Play Store on watch
- Wear Compose Material 1.2.1 - Wear OS Material Design
- Wear Compose Foundation 1.2.1 - Foundation components
- Core Splashscreen 1.0.1 - Splash screen API

## Configuration

**Environment:**
- Configuration via `local.properties` file (not committed to git)
- Required secrets: `DEEPGRAM_API_KEY` (loaded via BuildConfig)
- Local properties file present: Yes
- Gradle properties: `gradle.properties` (JVM args, AndroidX settings)

**Build:**
- Root build config: `build.gradle.kts`
- App module config: `app/build.gradle.kts`
- Wear module config: `wear/build.gradle.kts`
- Common module config: `common/build.gradle.kts`
- Settings: `settings.gradle.kts`
- Version catalog: `gradle/libs.versions.toml`
- ProGuard rules: `proguard-rules.pro` (minification disabled in current config)

**Build Features:**
- Compose enabled: Yes
- BuildConfig enabled: Yes (for injecting API keys)
- ViewBinding: Not used

**NDK Configuration (Wear module only):**
- ABI filters: armeabi-v7a, arm64-v8a, x86, x86_64
- TensorFlow Lite assets: No compression for .tflite files

## Platform Requirements

**Development:**
- Android Studio (Arctic Fox or later recommended for Compose)
- JDK 11 or higher
- Android SDK 36
- Gradle 8.13.2+
- Physical Wear OS device or emulator (API 30+) for wear module testing

**Production:**
- Phone app: Android 7.0 (API 24) and above
- Wear app: Android 11 (API 30) and above (Wear OS 3.0+)
- Bluetooth required for phone-watch communication
- Internet connection required for Deepgram TTS API

**Multi-Module Structure:**
- `:app` - Main phone application
- `:wear` - Wear OS companion app
- `:common` - Shared code between app and wear (Wearable API wrappers)

---

*Stack analysis: 2026-02-15*

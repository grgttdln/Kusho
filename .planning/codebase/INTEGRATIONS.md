# External Integrations

**Analysis Date:** 2026-02-15

## APIs & External Services

**Speech & Audio:**
- Deepgram Text-to-Speech API - Natural voice synthesis for learning prompts
  - SDK/Client: Retrofit 2.9.0 + OkHttp 4.12.0
  - Base URL: `https://api.deepgram.com/`
  - Endpoint: `POST /v1/speak?model=aura-2-asteria-en`
  - Auth: Token-based (Bearer token in Authorization header)
  - Environment variable: `DEEPGRAM_API_KEY` (stored in `local.properties`)
  - Implementation: `app/src/main/java/com/example/app/speech/DeepgramTTSManager.kt`
  - Voice model: aura-2-asteria-en (clear, natural female voice)
  - Playback speed: 0.9x (slowed for children)
  - Caching: Audio responses cached locally in `deepgram_audio/` cache directory
  - Usage: Learn Mode question prompts, feedback messages

**Android System TTS:**
- Android TextToSpeech (Wear OS) - Fallback voice synthesis on watch
  - Implementation: `wear/src/main/java/com/example/kusho/speech/TextToSpeechManager.kt`
  - No external credentials required
  - Used for: Watch-side voice feedback

## Data Storage

**Databases:**
- Room (SQLite) - Local embedded database
  - Connection: Direct local access (no connection string)
  - Client: Room 2.6.1 with KSP annotation processor
  - Database file: `kusho_database`
  - Location: `app/src/main/java/com/example/app/data/AppDatabase.kt`
  - Schema version: 10
  - Migration strategy: `fallbackToDestructiveMigration()` enabled
  - Entities: 12 tables (User, Word, Student, Class, Enrollment, Activity, Set, SetWord, ActivitySet, StudentTeacher, LearnerProfileAnnotation, StudentSetProgress)
  - DAOs: 9 data access objects in `app/src/main/java/com/example/app/data/dao/`
  - Repositories: 8 repositories in `app/src/main/java/com/example/app/data/repository/`

**File Storage:**
- Local filesystem only
  - Deepgram audio cache: `context.cacheDir/deepgram_audio/`
  - TensorFlow Lite models: `wear/src/main/assets/*.tflite`
  - Audio feedback files: `app/src/main/res/raw/*.mp3`

**Caching:**
- None (beyond Deepgram audio cache and Coil image cache)

**Preferences:**
- DataStore Preferences 1.0.0 - Session management and user preferences
  - Type-safe key-value storage
  - Replaces SharedPreferences

## Authentication & Identity

**Auth Provider:**
- Custom/Local authentication
  - Implementation: Room database User entity
  - No external authentication service detected
  - User management: `app/src/main/java/com/example/app/data/repository/UserRepository.kt`
  - Session tracking: DataStore Preferences

## Monitoring & Observability

**Error Tracking:**
- None (no Sentry, Crashlytics, or similar detected)

**Logs:**
- Android Logcat only
  - HTTP logging: OkHttp LoggingInterceptor (level: BODY)
  - Custom logging tags: "DeepgramTTSManager", "WatchConnectionMgr"

## CI/CD & Deployment

**Hosting:**
- Not applicable (Android app distributed via APK/Play Store)

**CI Pipeline:**
- None detected (no `.github/workflows/`, `.gitlab-ci.yml`, or similar)

## Environment Configuration

**Required env vars:**
- `DEEPGRAM_API_KEY` - API key for Deepgram TTS service
  - Stored in: `local.properties` (not committed to version control)
  - Injected via: BuildConfig field in `app/build.gradle.kts`
  - Default fallback: Empty string (TTS manager checks `isConfigured()`)

**Secrets location:**
- `local.properties` file present (contains API keys, not committed to git)
- No cloud-based secret management detected

**Permissions (AndroidManifest.xml):**
- `INTERNET` - Required for Deepgram API calls
- `ACCESS_NETWORK_STATE` - Network connectivity checks
- `BLUETOOTH` - Wear OS device communication
- `BLUETOOTH_ADMIN` - Bluetooth state management
- `BLUETOOTH_CONNECT` - Connecting to Wear OS devices
- `POST_NOTIFICATIONS` - Push notifications for watch connection status
- `VIBRATE` - Haptic feedback for notifications

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

## Device-to-Device Communication

**Wearable Data Layer API:**
- Google Play Services Wearable 19.0.0 - Phone ↔ Watch communication
  - Protocol: Wearable Data Layer (Google proprietary)
  - Manager: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt`
  - Clients used:
    - `NodeClient` - Device discovery
    - `MessageClient` - Bidirectional messaging
    - `CapabilityClient` - Feature detection
    - `DataClient` - Data synchronization (inferred from common module)
  - Message paths:
    - `/request_battery` - Request battery level from watch
    - Learn mode skip triggers
    - Letter input events from watch
    - Feedback dismissal events
    - Tutorial mode gesture results
  - Capability name: "kusho_wear_app"
  - Connection state tracking: BLUETOOTH_OFF, NO_WATCH, WATCH_PAIRED_NO_APP, WATCH_CONNECTED
  - Bluetooth state monitoring: BroadcastReceiver for Bluetooth adapter state changes
  - Play Store integration: `RemoteActivityHelper` for prompting watch app installation

## Machine Learning

**On-Device Inference:**
- TensorFlow Lite 2.17.0 - Local handwriting recognition
  - Model location: `wear/src/main/assets/` (5 models)
    - `complete_model_1.tflite`
    - `model2.tflite`
    - `tcn_multihead_model.tflite`
    - `tcn_multihead_model_CAPITAL_right.tflite`
    - `tcn_multihead_model_small_right.tflite`
  - Classifier: `wear/src/main/java/com/example/kusho/ml/AirWritingClassifier.kt`
  - Input: Motion sensor data from Wear OS (accelerometer, gyroscope)
  - Output: Letter classification for air-writing recognition
  - No cloud-based ML APIs

**Sensor Integration:**
- Motion sensors (Wear OS): `wear/src/main/java/com/example/kusho/sensors/MotionSensorManager.kt`
- Shake detection: `wear/src/main/java/com/example/kusho/sensors/ShakeDetector.kt`

## Media & Assets

**Audio Files:**
- Pre-recorded MP3 feedback: `app/src/main/res/raw/*.mp3`
  - Correct feedback: "You did a great job", "Great effort on that task"
  - Wrong feedback: "Try again", "Don't worry try again", "You can do better"
  - Session complete: `finish.mp3`
- MediaPlayer used for local audio playback
- Deepgram-synthesized audio cached as MP3 files

**Image Loading:**
- Coil Compose 2.5.0 - Asynchronous image loading
  - Automatic caching (memory and disk)
  - Compose integration

---

*Integration audit: 2026-02-15*

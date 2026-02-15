# Codebase Concerns

**Analysis Date:** 2026-02-15

## Tech Debt

**Database Migration Strategy:**
- Issue: Using `.fallbackToDestructiveMigration()` instead of proper Room migrations
- Files: `app/src/main/java/com/example/app/data/AppDatabase.kt:89`
- Impact: All user data is lost on schema changes. Currently at version 10 with comment "Added sessionMode column to LearnerProfileAnnotation table"
- Fix approach: Implement proper Room migration paths between versions. Create Migration objects for each schema change to preserve user data during updates.

**Missing Error Feedback in UI:**
- Issue: Multiple TODO comments for showing error toast/snackbar when operations fail
- Files:
  - `app/src/main/java/com/example/app/ui/feature/classroom/StudentDetailsScreen.kt:118,507,600`
  - `app/src/main/java/com/example/app/ui/feature/classroom/ClassScreen.kt:431`
  - `app/src/main/java/com/example/app/ui/feature/classroom/EditClassScreen.kt:307,381`
  - `app/src/main/java/com/example/app/ui/feature/classroom/ClassDetailsScreen.kt:305`
- Impact: Users don't receive feedback when operations fail (image updates, student edits, class operations). Silent failures create confusion and appear as bugs.
- Fix approach: Implement consistent error handling with SnackbarHost or Toast messages. Create a shared ErrorHandler utility to centralize error display logic.

**Incomplete Dashboard Implementation:**
- Issue: TODO comment "Dashboard sections go here" indicates incomplete feature
- Files: `app/src/main/java/com/example/app/ui/feature/dashboard/DashboardScreen.kt:436`
- Impact: Dashboard may be missing planned functionality or sections
- Fix approach: Define dashboard sections requirements and implement missing components

**Tutorial Navigation Stub:**
- Issue: Empty onClick handler with TODO for tutorial navigation
- Files: `app/src/main/java/com/example/app/ui/feature/classroom/StudentDetailsScreen.kt:300`
- Impact: Tutorial navigation button is non-functional
- Fix approach: Implement tutorial navigation callback and route

**Generic Exception Catching:**
- Issue: Extensive use of `catch (e: Exception)` with only logging, no recovery
- Files:
  - `app/src/main/java/com/example/app/service/WatchConnectionManager.kt` (27+ occurrences)
  - `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeStateHolder.kt`
  - `wear/src/main/java/com/example/kusho/presentation/tutorial/TutorialModeStateHolder.kt`
  - `common/src/main/java/com/example/kusho/common/MessageService.kt`
- Impact: Broad exception catching masks specific errors, making debugging harder. Watch connectivity errors fail silently without user notification.
- Fix approach: Catch specific exceptions (IOException, SecurityException, etc.) and implement proper error recovery or user feedback for each case.

## Known Bugs

**Deprecated Android API Usage:**
- Symptoms: Using deprecated BLUETOOTH and BLUETOOTH_ADMIN permissions
- Files: `app/src/main/AndroidManifest.xml:5-6`
- Trigger: Android 12+ (API 31+) requires BLUETOOTH_CONNECT, BLUETOOTH_SCAN instead
- Workaround: BLUETOOTH_CONNECT is present, but old permissions still declared
- Fix: Remove deprecated BLUETOOTH and BLUETOOTH_ADMIN permissions, ensure proper runtime permission handling for Android 12+

## Security Considerations

**Password Security:**
- Risk: Password hashing uses 120,000 PBKDF2 iterations (below OWASP 2023 recommendation of 600,000)
- Files: `app/src/main/java/com/example/app/util/PasswordUtils.kt:28`
- Current mitigation: Using PBKDF2WithHmacSHA256 with salt, constant-time comparison
- Recommendations:
  - Increase iterations to 600,000 for new passwords
  - Consider Argon2 via a library for stronger protection
  - Implement Android Keystore for key management
  - Add application-level pepper (per comment in file)

**API Key Exposure Risk:**
- Risk: Deepgram API key loaded from local.properties and compiled into BuildConfig
- Files:
  - `app/build.gradle.kts:33-34`
  - `app/src/main/java/com/example/app/speech/DeepgramTTSManager.kt:77`
- Current mitigation: Key stored in local.properties (not committed), but still embedded in APK
- Recommendations:
  - Use backend proxy for API calls to avoid exposing key in APK
  - Implement key rotation strategy
  - Add rate limiting to prevent API abuse if key is extracted

**Missing Permission Runtime Checks:**
- Risk: Bluetooth and notification permissions declared but runtime checks may be incomplete
- Files: `app/src/main/AndroidManifest.xml:4-17`
- Current mitigation: Permission launchers exist in `DashboardScreen.kt:90-99`
- Recommendations: Audit all permission usages to ensure proper runtime checks before access. Add graceful degradation when permissions denied.

## Performance Bottlenecks

**Large Screen Components:**
- Problem: Multiple screens exceed 1000 lines, indicating high complexity
- Files:
  - `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` (1,167 lines)
  - `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (1,152 lines)
  - `app/src/main/java/com/example/app/service/WatchConnectionManager.kt` (940 lines)
  - `app/src/main/java/com/example/app/ui/feature/learn/tutorialmode/TutorialSessionScreen.kt` (680 lines)
  - `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt` (638 lines)
  - `app/src/main/java/com/example/app/ui/components/dashboard/ActivityProgress.kt` (540 lines)
- Cause: Monolithic Composables with business logic, UI, and state management mixed together
- Improvement path:
  - Extract business logic to ViewModels
  - Break down large Composables into smaller, reusable components
  - Separate concerns: UI, state, and data logic

**ML Model Loading:**
- Problem: Multiple large TensorFlow Lite models loaded into memory
- Files:
  - `wear/src/main/assets/tcn_multihead_model.tflite` (605 KB)
  - `wear/src/main/assets/tcn_multihead_model_CAPITAL_right.tflite` (605 KB)
  - `wear/src/main/assets/tcn_multihead_model_small_right.tflite` (605 KB)
  - `wear/src/main/assets/complete_model_1.tflite` (123 KB)
  - `wear/src/main/assets/model2.tflite` (120 KB)
- Cause: Total ~2 MB of models for air-writing recognition on Wear OS with limited memory
- Impact: Memory pressure on smartwatch, potential OOM errors
- Improvement path:
  - Load only required model based on mode (uppercase/lowercase)
  - Implement lazy loading and unloading of unused models
  - Consider model quantization to reduce size
  - Monitor memory usage with profiler

**Watch Connection Polling:**
- Problem: Active polling with delays for watch connection status
- Files: `app/src/main/java/com/example/app/service/WatchConnectionManager.kt:209,317,568`
- Cause: Polling every 2 seconds for watch status, 1-2 second delays between retries
- Impact: Battery drain on phone, unnecessary CPU cycles
- Improvement path: Use reactive listeners (CapabilityClient listeners, NodeClient listeners) instead of polling. Only poll on demand or when connection state changes.

**Deepgram API Caching:**
- Problem: Text-to-speech caching uses simple hashCode for cache keys
- Files: `app/src/main/java/com/example/app/speech/DeepgramTTSManager.kt:144`
- Cause: `text.hashCode().toString()` can have collisions with different text
- Impact: Rare cache misses or serving wrong audio for similar hash codes
- Improvement path: Use MD5/SHA hash of text or UUID-based naming for guaranteed uniqueness

## Fragile Areas

**Watch Communication Layer:**
- Files:
  - `app/src/main/java/com/example/app/service/WatchConnectionManager.kt` (940 lines)
  - `wear/src/main/java/com/example/kusho/presentation/service/PhoneCommunicationManager.kt` (581 lines)
- Why fragile: Complex bidirectional message passing between phone and watch with multiple flows, timestamps, and state synchronization. Heavy exception catching suggests error-prone operations.
- Safe modification: Always update message path constants in both app and wear modules simultaneously. Test all message flows in both directions. Add integration tests for message delivery.
- Test coverage: No test files found for WatchConnectionManager. Critical gap.

**LearnMode Session Screens:**
- Files:
  - `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` (1,167 lines)
  - `wear/src/main/java/com/example/kusho/presentation/learn/LearnModeScreen.kt` (1,152 lines)
- Why fragile: Massive composables with nested state management, media playback, network checks, TTS integration, and watch synchronization. Multiple LaunchedEffects and side effects make behavior hard to predict.
- Safe modification: Extract ViewModels for state management. Break UI into smaller components. Test each component in isolation. Document side effect dependencies.
- Test coverage: No test files found for LearnModeSessionScreen. High-risk area.

**Case-Sensitive Letter Matching Logic:**
- Files: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt:68-86`
- Why fragile: Custom logic for case-insensitive matching of similar letters (c,k,o,p,s,u,v,w,x,z). Hardcoded set of letters with special handling. Air-writing recognition depends on this being correct.
- Safe modification: Document why each letter is in `similarCaseLetters` set. Add unit tests for all letter combinations. Consider making this configurable per activity/set.
- Test coverage: No unit tests found for `isLetterMatch` function.

**Database Schema Evolution:**
- Files: `app/src/main/java/com/example/app/data/AppDatabase.kt:52-54`
- Why fragile: Currently at version 10 with destructive migration. Schema includes 11 entities with complex relationships. Any schema change loses all user data.
- Safe modification: Never change schema without proper migration. Test migrations with real data. Create migration tests. Consider export schema to track changes.
- Test coverage: No migration tests found. High risk for data loss.

## Scaling Limits

**Local Database Storage:**
- Current capacity: SQLite database with no size limits configured
- Limit: Android internal storage limits, typically gigabytes available
- Scaling path:
  - Implement data archiving for old activities/sessions
  - Add pagination for large lists (students, activities, words)
  - Consider backend sync for data backup and multi-device support

**TTS Audio Cache:**
- Current capacity: Unlimited cache in `app/cache/deepgram_audio/`
- Limit: System may clear cache when storage is low
- Scaling path: Implement cache size limit (e.g., 50 MB max), LRU eviction, manual cache clearing in settings

**Image Storage:**
- Current capacity: Word bank images stored in internal storage with 1024px max dimension, 85% JPEG quality
- Files: `app/src/main/java/com/example/app/util/ImageStorageManager.kt:24-25`
- Limit: Each image ~100-300 KB, internal storage fills with many words
- Scaling path:
  - Implement cleanup of orphaned images (method exists but not called)
  - Add storage usage monitoring in UI
  - Consider cloud storage for shared word banks
  - Reduce max dimension or quality if needed

**Watch Message Queue:**
- Current capacity: Uses Google Play Services Wearable DataLayer (no explicit queue management)
- Limit: Message delivery is best-effort, can fail or be delayed
- Scaling path: Implement message acknowledgment, retry logic, and queue persistence for critical messages

## Dependencies at Risk

**Deprecated Compose Material:**
- Risk: Using androidx.compose.material:material-icons-extended without version pinning
- Files: `app/build.gradle.kts:71`
- Impact: Material 2 icons while using Material 3, potential version conflicts
- Migration plan: Migrate to Material 3 icons when available, or pin version to prevent breaking changes

**Play Services Wearable:**
- Risk: Dependency on Google Play Services for watch communication
- Files: `app/build.gradle.kts:75`
- Impact: App requires Google Play Services, won't work on devices without it (e.g., some international markets)
- Migration plan: Consider alternative communication channels (Bluetooth direct, WebSockets) or graceful degradation

## Missing Critical Features

**Test Suite:**
- Problem: Only example/placeholder test files exist
- Blocks: Safe refactoring, regression prevention, CI/CD pipeline
- Files:
  - `app/src/test/java/com/example/app/ExampleUnitTest.kt` (placeholder)
  - `app/src/androidTest/java/com/example/app/ExampleInstrumentedTest.kt` (placeholder)
- Priority: High - Need unit tests for ViewModels, repositories, utilities; UI tests for critical flows

**Error Recovery:**
- Problem: No offline mode or data recovery mechanisms
- Blocks: User experience when network unavailable (TTS fails), watch disconnects (session interruption)
- Priority: Medium - Implement graceful degradation (local TTS fallback, resume session after watch reconnect)

**Data Backup/Sync:**
- Problem: All data stored locally, no cloud backup or multi-device sync
- Blocks: User data recovery on device loss, seamless experience across devices
- Priority: Medium - Implement backend API with authentication and data sync

**Analytics/Monitoring:**
- Problem: No crash reporting or analytics integration
- Blocks: Understanding user issues, tracking feature usage, monitoring app health
- Priority: Medium - Add Firebase Crashlytics or similar for production error tracking

## Test Coverage Gaps

**Critical Untested Components:**
- `app/src/main/java/com/example/app/service/WatchConnectionManager.kt` (940 lines, 0 tests)
- `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSessionScreen.kt` (1,167 lines, 0 tests)
- `app/src/main/java/com/example/app/util/PasswordUtils.kt` (122 lines, security-critical, 0 tests)
- `app/src/main/java/com/example/app/util/ImageStorageManager.kt` (229 lines, 0 tests)
- `app/src/main/java/com/example/app/speech/DeepgramTTSManager.kt` (314 lines, 0 tests)
- All ViewModel classes (no test files found)
- All Repository classes (no test files found)
- All DAO classes (Room generates implementations, but queries untested)

**Risk:** High - Core business logic and critical paths have zero test coverage. Refactoring is dangerous. Regressions are likely.

**Priority:** High - Start with unit tests for ViewModels, repositories, and utility classes. Add integration tests for database operations. Create UI tests for main user flows (login, create student, learn mode session).

---

*Concerns audit: 2026-02-15*

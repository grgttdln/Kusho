# Testing Patterns

**Analysis Date:** 2026-02-15

## Test Framework

**Runner:**
- JUnit 4
- Config: Gradle dependencies in `app/build.gradle.kts`

**Assertion Library:**
- JUnit assertions (`assertEquals`, `assertTrue`, etc.)
- Standard library: `org.junit.Assert.*`

**Android Testing:**
- AndroidJUnit4 runner for instrumented tests
- Espresso for UI testing

**Run Commands:**
```bash
./gradlew test              # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests on device/emulator
./gradlew testDebugUnitTest     # Run debug unit tests
```

## Test File Organization

**Location:**
- Unit tests: Co-located in separate source set
  - `app/src/test/java/com/example/app/`
- Instrumented tests: Separate source set
  - `app/src/androidTest/java/com/example/app/`

**Naming:**
- Test classes append "Test": `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`
- No other test files currently exist beyond examples

**Structure:**
```
app/src/
├── main/java/com/example/app/          # Application code
├── test/java/com/example/app/          # Unit tests (JVM)
└── androidTest/java/com/example/app/   # Instrumented tests (Android device)
```

## Test Structure

**Suite Organization:**
```kotlin
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
```

**Instrumented Test:**
```kotlin
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.app", appContext.packageName)
    }
}
```

**Patterns:**
- Test method names use snake_case: `addition_isCorrect()`, `useAppContext()`
- No setup/teardown methods in example tests
- Single assertion per test method
- No BeforeEach/AfterEach usage detected

## Mocking

**Framework:**
- No mocking framework detected in dependencies
- No Mockito, MockK, or similar libraries configured

**Patterns:**
- Not applicable - no mocking patterns established

**What to Mock:**
- Guidelines not established (no existing tests with mocks)

**What NOT to Mock:**
- Guidelines not established

## Fixtures and Factories

**Test Data:**
- No test fixtures or factory patterns detected
- No test data builders or object mothers found

**Location:**
- No dedicated fixtures directory

## Coverage

**Requirements:** None enforced

**Configuration:**
- No JaCoCo or coverage plugin configured in build files
- No coverage thresholds set

**View Coverage:**
```bash
# Not configured - would need to add JaCoCo plugin
./gradlew jacocoTestReport  # (not available)
```

## Test Types

**Unit Tests:**
- Location: `app/src/test/java/`
- Framework: JUnit 4
- Scope: Pure Kotlin/Java logic, no Android dependencies
- Current state: Only example test exists (`ExampleUnitTest.kt`)

**Instrumented Tests:**
- Location: `app/src/androidTest/java/`
- Framework: AndroidJUnit4 + Espresso
- Scope: Tests requiring Android framework (Context, Activities, etc.)
- Current state: Only example test exists (`ExampleInstrumentedTest.kt`)

**Integration Tests:**
- Not established as separate category
- Would use instrumented tests for Room database integration

**Compose UI Tests:**
- Dependencies present: `libs.ui.test.junit4`, `libs.ui.test.manifest`
- No existing Compose UI tests
- Debug implementation includes: `libs.ui.tooling`, `libs.ui.test.manifest`

**E2E Tests:**
- Not used

## Common Patterns

**Async Testing:**
- Not established - no coroutine test patterns found
- Would need `kotlinx-coroutines-test` for coroutine testing (not in dependencies)

**Database Testing:**
- Not established
- Would use in-memory Room database: `Room.inMemoryDatabaseBuilder()`
- No existing DAO or Repository tests

**ViewModel Testing:**
- Not established
- Would need `androidx.arch.core:core-testing` for LiveData/StateFlow testing

**Compose Testing:**
- Framework available but not used
- Pattern would be:
```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun myTest() {
    composeTestRule.setContent {
        MyComposable()
    }
    composeTestRule.onNodeWithText("Hello").assertExists()
}
```

## Current Test Coverage

**Areas Covered:**
- Basic JUnit setup verification (example tests only)
- Context availability verification (instrumented test)

**Areas Not Covered:**
- Authentication logic (`LoginViewModel`, `SignUpViewModel`)
- Repository layer (all repositories untested)
- Database operations (all DAOs untested)
- Password utilities (`PasswordUtils.kt`)
- Validation logic (`WordValidator.kt`)
- UI components (no Compose UI tests)
- ViewModels (no ViewModel tests)
- Business logic in services (`WatchConnectionManager`, `MessageService`)

## Testing Configuration

**Test Dependencies (from `app/build.gradle.kts`):**
```kotlin
testImplementation(libs.junit)                      // JUnit 4
androidTestImplementation(libs.ext.junit)           // AndroidX JUnit extensions
androidTestImplementation(libs.espresso.core)       // Espresso UI testing
androidTestImplementation(platform(libs.compose.bom))
androidTestImplementation(libs.ui.test.junit4)      // Compose testing
debugImplementation(libs.ui.tooling)                // Compose tooling
debugImplementation(libs.ui.test.manifest)          // Test manifest
```

**Test Instrumentation Runner:**
```kotlin
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

## Recommendations for New Tests

**When adding Repository tests:**
- Use in-memory Room database
- Test sealed class result types
- Verify error handling and validation
- Test suspend functions with runBlocking or runTest

**When adding ViewModel tests:**
- Add `kotlinx-coroutines-test` dependency
- Use `TestCoroutineDispatcher` or `UnconfinedTestDispatcher`
- Test StateFlow emissions
- Verify loading states and error states

**When adding Compose UI tests:**
- Use `createComposeRule()`
- Test user interactions (clicks, text input)
- Verify state updates in UI
- Test navigation flows

**When adding DAO tests:**
- Use in-memory database for fast, isolated tests
- Test CRUD operations
- Test Flow emissions for reactive queries
- Verify unique constraints and foreign keys

---

*Testing analysis: 2026-02-15*

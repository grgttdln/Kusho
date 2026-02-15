# Coding Conventions

**Analysis Date:** 2026-02-15

## Naming Patterns

**Files:**
- PascalCase for classes and screens: `LoginViewModel.kt`, `UserRepository.kt`, `StudentDetailsScreen.kt`
- Component files match their primary composable: `PrimaryButton.kt`, `BottomNavBar.kt`
- Entity files match database table names: `User.kt`, `Student.kt`, `Class.kt`
- DAO files append "Dao": `UserDao.kt`, `ClassDao.kt`
- Repository files append "Repository": `UserRepository.kt`, `ClassRepository.kt`

**Functions:**
- camelCase for all functions: `login()`, `signUp()`, `getUserById()`, `createClass()`
- Composable functions use PascalCase: `LoginScreen()`, `PrimaryButton()`, `ClassCard()`
- Boolean functions use "is" or "has" prefix: `isEmailExists()`, `verifyPassword()`

**Variables:**
- camelCase for variables: `userId`, `className`, `passwordHash`, `staySignedIn`
- Backing properties use underscore prefix: `_uiState` (private), `uiState` (public)
- Database columns match Kotlin property names in camelCase

**Types:**
- PascalCase for data classes: `LoginUiState`, `PredictionResult`, `SensorSample`
- Sealed classes for result types: `LoginResult`, `SignUpResult`, `ClassOperationResult`
- Enum-like sealed classes for navigation: `Screen.Login`, `Screen.SignUp`

## Code Style

**Formatting:**
- No explicit formatter configuration detected (using Kotlin/Android Studio defaults)
- Indentation: 4 spaces
- Line length: Varies, some files exceed 120 characters
- Trailing commas: Not consistently used

**Linting:**
- No ktlint or detekt configuration files found
- No explicit linting rules enforced in build files

## Import Organization

**Order:**
1. Android framework imports (`android.*`, `androidx.*`)
2. Third-party libraries (`com.google.*`, `kotlinx.*`)
3. Internal imports (`com.example.app.*`, `com.example.kusho.*`)

**Example:**
```kotlin
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.User
import kotlinx.coroutines.flow.MutableStateFlow
```

**Path Aliases:**
- No custom path aliases detected
- All imports use full package paths

## Error Handling

**Patterns:**

**Sealed Classes for Results:**
```kotlin
sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
```

**Try-Catch in Repository Layer:**
```kotlin
suspend fun login(email: String, password: String): LoginResult = withContext(Dispatchers.IO) {
    try {
        val user = userDao.getUserByEmail(email.lowercase().trim())
            ?: return@withContext LoginResult.Error("Invalid email or password")
        // ... validation
        LoginResult.Success(user)
    } catch (e: Exception) {
        LoginResult.Error("Login failed: ${e.message}")
    }
}
```

**When Expression for Result Handling:**
```kotlin
when (val result = userRepository.login(email, password)) {
    is UserRepository.LoginResult.Success -> {
        // handle success
    }
    is UserRepository.LoginResult.Error -> {
        // handle error
    }
}
```

**Validation with Early Returns:**
- Validate input at function start
- Return early with error messages
- Example in `UserRepository.signUp()` and `LoginViewModel.login()`

## Logging

**Framework:** None (no Timber or structured logging)

**Patterns:**
- Use `Log.d()`, `Log.e()`, `Log.w()` from `android.util.Log`
- Found in `WatchConnectionManager.kt` for Bluetooth debugging
- Print statements for development: `println("Deepgram API Key configured: ...")`
- No consistent logging strategy across codebase

## Comments

**When to Comment:**
- Document complex algorithms and security-critical code
- Explain "why" not "what" for business logic
- Header comments on files with multi-paragraph explanations
- Parameter documentation in KDoc style

**KDoc/Documentation:**
```kotlin
/**
 * Repository for User data operations.
 *
 * This class acts as a single source of truth for user data and
 * abstracts the data sources from the rest of the app.
 *
 * All database operations are performed on the IO dispatcher
 * to avoid blocking the main thread.
 */
class UserRepository(private val userDao: UserDao) { ... }

/**
 * Attempt to log in a user.
 *
 * @param email User's email
 * @param password User's password (plain text - will be hashed for comparison)
 * @param staySignedIn Whether to keep the user signed in (for future implementation)
 * @param onSuccess Callback when login is successful, provides the logged-in user
 */
fun login(email: String, password: String, staySignedIn: Boolean = false, onSuccess: (User) -> Unit)
```

**Inline Comments:**
- Used sparingly for clarification
- Security notes on sensitive fields: `// Store the hashed password, never plain text`
- Step-by-step explanations in complex flows

## Function Design

**Size:**
- Composable screens can be 500+ lines (e.g., `LearnModeSessionScreen.kt` at 1167 lines)
- Repository methods typically 20-50 lines
- ViewModel functions 30-100 lines
- Utility functions under 30 lines

**Parameters:**
- Named parameters for clarity
- Default values for optional parameters: `staySignedIn: Boolean = false`
- Callbacks as function parameters: `onSuccess: (User) -> Unit`
- Composables use `modifier: Modifier = Modifier` as first or last parameter

**Return Values:**
- Use sealed classes for operation results
- Suspend functions return result types or nullable values
- DAOs return `Flow<T>` for reactive data or suspend functions for one-time queries
- Boolean returns for validation: `isEmailExists(): Boolean`

## Module Design

**Exports:**
- All classes/functions are package-visible by default
- Repository classes expose public methods for UI layer
- DAOs are interfaces with Room annotations
- Entities are data classes with Room annotations

**Barrel Files:**
- Not used
- Each file contains a single primary class/composable
- Navigation uses sealed class pattern in `Screen.kt`

## Database Conventions

**Room Entity Design:**
- `@Entity` annotation with `tableName` parameter
- `@PrimaryKey(autoGenerate = true)` for auto-incrementing IDs
- Use `@Index` for unique constraints: `indices = [Index(value = ["email"], unique = true)]`
- Timestamp fields use `System.currentTimeMillis()` as default

**DAO Design:**
- Interface annotated with `@Dao`
- All methods are suspend functions
- Use `OnConflictStrategy.ABORT` for inserts
- Return `Flow<T>` for reactive queries
- Return nullable types for single item queries: `suspend fun getUserById(id: Long): User?`

**Repository Pattern:**
- Repository wraps DAO and performs business logic
- All database calls use `withContext(Dispatchers.IO)`
- Input validation before database operations
- Transform lowercase/trim on email fields

## Compose UI Conventions

**State Management:**
- ViewModels expose `StateFlow` for UI state
- Use `MutableStateFlow` privately, expose as `StateFlow` publicly
- Collect state with `collectAsState()` in composables
- Use `remember` for local component state

**Composable Structure:**
```kotlin
@Composable
fun ComponentName(
    modifier: Modifier = Modifier,
    viewModel: SomeViewModel = viewModel(),
    onAction: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(...) { paddingValues ->
        // Content
    }
}
```

**Color Usage:**
- Hardcoded hex colors: `Color(0xFF49A9FF)`
- Use `MaterialTheme.typography` for text styles
- Transparent backgrounds: `Color.Transparent`

## Coroutines Conventions

**Scope:**
- Use `viewModelScope.launch` in ViewModels
- Repository functions are suspend functions
- DAO functions are suspend functions

**Dispatchers:**
- `Dispatchers.IO` for database operations
- `Dispatchers.Main` implicit for UI updates
- Wrap DAO calls with `withContext(Dispatchers.IO)`

**Flow Usage:**
- DAOs return `Flow<List<T>>` for reactive queries
- ViewModels convert to `StateFlow` for UI consumption
- Use `.asStateFlow()` to expose immutable state

## Security Conventions

**Password Handling:**
- Never store plain text passwords
- Use PBKDF2WithHmacSHA256 for hashing (see `PasswordUtils.kt`)
- Generate unique salt per user: `generateSalt()`
- 120,000 iterations for PBKDF2
- Constant-time comparison to prevent timing attacks

**Sensitive Data:**
- API keys loaded from `local.properties` (not committed)
- BuildConfig fields for secrets: `buildConfigField("String", "DEEPGRAM_API_KEY", ...)`
- Email addresses normalized: `email.lowercase().trim()`

---

*Convention analysis: 2026-02-15*

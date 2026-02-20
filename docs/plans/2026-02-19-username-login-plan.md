# Username-Based Login Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace email-based auth with username-based auth, remove the school field entirely.

**Architecture:** Rename `email` → `username` across data layer (entity, DAO, repository, session manager) and UI layer (signup/login screens + ViewModels). Bump DB version with destructive migration.

**Tech Stack:** Kotlin, Jetpack Compose, Room, SharedPreferences

---

### Task 1: Update User Entity

**Files:**
- Modify: `app/src/main/java/com/example/app/data/entity/User.kt`

**Step 1: Replace the User entity**

Replace the entire file content with:

```kotlin
package com.example.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val username: String,

    val name: String,

    // Store the hashed password, never plain text
    val passwordHash: String,

    // Salt used for hashing - each user has a unique salt
    val salt: String,

    // Timestamp for when the account was created
    val createdAt: Long = System.currentTimeMillis()
)
```

Changes: `email` → `username`, removed `school`, updated index, removed KDoc (the code is self-documenting).

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/entity/User.kt
git commit -m "refactor: rename email to username in User entity, remove school field"
```

---

### Task 2: Update UserDao

**Files:**
- Modify: `app/src/main/java/com/example/app/data/dao/UserDao.kt`

**Step 1: Replace the UserDao interface**

Replace the entire file content with:

```kotlin
package com.example.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app.data.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username LIMIT 1)")
    suspend fun isUsernameExists(username: String): Boolean

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long): Int

    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun updateUserProfile(id: Long, name: String): Int
}
```

Changes: `getUserByEmail` → `getUserByUsername`, `isEmailExists` → `isUsernameExists`, `updateUserProfile` drops `school` parameter, all SQL queries updated.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/dao/UserDao.kt
git commit -m "refactor: rename email to username in UserDao, remove school from updateUserProfile"
```

---

### Task 3: Update UserRepository

**Files:**
- Modify: `app/src/main/java/com/example/app/data/repository/UserRepository.kt`

**Step 1: Replace the UserRepository class**

Replace the entire file content with:

```kotlin
package com.example.app.data.repository

import com.example.app.data.dao.UserDao
import com.example.app.data.entity.User
import com.example.app.util.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    sealed class SignUpResult {
        data class Success(val userId: Long) : SignUpResult()
        data class Error(val message: String) : SignUpResult()
    }

    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    suspend fun signUp(
        username: String,
        name: String,
        password: String
    ): SignUpResult = withContext(Dispatchers.IO) {
        try {
            if (username.isBlank()) {
                return@withContext SignUpResult.Error("Username is required")
            }
            if (name.isBlank()) {
                return@withContext SignUpResult.Error("Name is required")
            }

            val (isValid, errorMessage) = PasswordUtils.validatePasswordStrength(password)
            if (!isValid) {
                return@withContext SignUpResult.Error(errorMessage ?: "Invalid password")
            }

            if (userDao.isUsernameExists(username.trim())) {
                return@withContext SignUpResult.Error("Username is already taken")
            }

            val salt = PasswordUtils.generateSalt()
            val passwordHash = PasswordUtils.hashPassword(password, salt)

            val user = User(
                username = username.trim(),
                name = name.trim(),
                passwordHash = passwordHash,
                salt = salt
            )

            val userId = userDao.insertUser(user)
            SignUpResult.Success(userId)

        } catch (e: Exception) {
            SignUpResult.Error("Failed to create account: ${e.message}")
        }
    }

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByUsername(username.trim())
                ?: return@withContext LoginResult.Error("Invalid username or password")

            val isPasswordValid = PasswordUtils.verifyPassword(
                password = password,
                storedHash = user.passwordHash,
                salt = user.salt
            )

            if (!isPasswordValid) {
                return@withContext LoginResult.Error("Invalid username or password")
            }

            LoginResult.Success(user)

        } catch (e: Exception) {
            LoginResult.Error("Login failed: ${e.message}")
        }
    }

    suspend fun getUserById(id: Long): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(id)
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim())
    }

    fun getAllUsersFlow(): Flow<List<User>> = userDao.getAllUsersFlow()

    suspend fun deleteUser(id: Long): Boolean = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id) > 0
    }
}
```

Changes: all `email` params → `username`, removed `school` param from `signUp`, removed `.lowercase()` calls (usernames are case-sensitive by default — unlike emails), updated error messages, `getUserByEmail` → `getUserByUsername`.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/repository/UserRepository.kt
git commit -m "refactor: rename email to username in UserRepository, remove school param"
```

---

### Task 4: Bump Database Version

**Files:**
- Modify: `app/src/main/java/com/example/app/data/AppDatabase.kt`

**Step 1: Update the version number**

In `AppDatabase.kt`, change line 58:

```
version = 14, // Added activityId to annotations and summaries
```

to:

```
version = 15, // Renamed email to username, removed school field
```

Note: `.fallbackToDestructiveMigration()` is already present on line 97, so no other changes needed.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/AppDatabase.kt
git commit -m "chore: bump database version to 15 for username schema change"
```

---

### Task 5: Update SessionManager

**Files:**
- Modify: `app/src/main/java/com/example/app/data/SessionManager.kt`

**Step 1: Replace the SessionManager class**

Replace the entire file content with:

```kotlin
package com.example.app.data

import android.content.Context
import android.content.SharedPreferences
import com.example.app.data.entity.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadSavedUser()
    }

    fun saveUserSession(user: User, staySignedIn: Boolean = true) {
        _currentUser.value = user

        if (staySignedIn) {
            prefs.edit().apply {
                putLong(KEY_USER_ID, user.id)
                putString(KEY_USERNAME, user.username)
                putString(KEY_NAME, user.name)
                putBoolean(KEY_IS_LOGGED_IN, true)
                putBoolean(KEY_STAY_SIGNED_IN, true)
                apply()
            }
        } else {
            prefs.edit().clear().apply()
        }
    }

    private fun loadSavedUser() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val staySignedIn = prefs.getBoolean(KEY_STAY_SIGNED_IN, false)

        if (isLoggedIn && staySignedIn) {
            val userId = prefs.getLong(KEY_USER_ID, 0)
            val username = prefs.getString(KEY_USERNAME, null)
            val name = prefs.getString(KEY_NAME, null)

            if (userId != 0L && username != null && name != null) {
                _currentUser.value = User(
                    id = userId,
                    username = username,
                    name = name,
                    passwordHash = "",
                    salt = ""
                )
            }
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val staySignedIn = prefs.getBoolean(KEY_STAY_SIGNED_IN, false)
        return isLoggedIn && staySignedIn
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0)
    }

    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "Guest") ?: "Guest"
    }

    companion object {
        private const val PREFS_NAME = "kusho_user_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NAME = "name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_STAY_SIGNED_IN = "stay_signed_in"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
```

Changes: `KEY_EMAIL` → `KEY_USERNAME`, removed `KEY_SCHOOL`, `user.email` → `user.username`, removed `school` from `loadSavedUser` and `saveUserSession`, removed `school` null check.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/data/SessionManager.kt
git commit -m "refactor: rename email to username in SessionManager, remove school"
```

---

### Task 6: Update SignUpViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpViewModel.kt`

**Step 1: Replace the SignUpViewModel class**

Replace the entire file content with:

```kotlin
package com.example.app.ui.feature.auth.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val userRepository = UserRepository(database.userDao())
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(
        username: String,
        name: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Passwords do not match"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = userRepository.signUp(username, name, password)) {
                is UserRepository.SignUpResult.Success -> {
                    val newUser = userRepository.getUserById(result.userId)
                    newUser?.let { user ->
                        sessionManager.saveUserSession(user, staySignedIn = true)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        userId = result.userId,
                        errorMessage = null
                    )
                    onSuccess()
                }
                is UserRepository.SignUpResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetState() {
        _uiState.value = SignUpUiState()
    }
}

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val userId: Long? = null,
    val errorMessage: String? = null
)
```

Changes: `email` param → `username`, removed `school` param, removed `isValidEmail` function and its call, removed `android.util.Patterns` import.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpViewModel.kt
git commit -m "refactor: rename email to username in SignUpViewModel, remove school and email validation"
```

---

### Task 7: Update SignUpScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpScreen.kt`

**Step 1: Update the SignUpScreen composable**

Replace the entire file content with:

```kotlin
package com.example.app.ui.feature.auth.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.theme.KushoTheme

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = viewModel(),
    onSignUpSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.dis_study),
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Image(
                painter = painterResource(id = R.drawable.title_register),
                contentDescription = "Welcome to Kusho",
                modifier = Modifier
                    .width(300.dp)
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Sign up to access Kusho's interactive\nlearning tools.",
                color = Color(0xFF2D2D2D),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            TextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Create a Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF49A9FF),
                    unfocusedIndicatorColor = Color(0xFF49A9FF),
                    focusedTextColor = Color(0xFF2D2D2D),
                    unfocusedTextColor = Color(0xFF2D2D2D)
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter your Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF49A9FF),
                    unfocusedIndicatorColor = Color(0xFF49A9FF),
                    focusedTextColor = Color(0xFF2D2D2D),
                    unfocusedTextColor = Color(0xFF2D2D2D)
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Create a Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF49A9FF)
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF49A9FF),
                    unfocusedIndicatorColor = Color(0xFF49A9FF),
                    focusedTextColor = Color(0xFF2D2D2D),
                    unfocusedTextColor = Color(0xFF2D2D2D)
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Re-enter your Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF49A9FF)
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF49A9FF),
                    unfocusedIndicatorColor = Color(0xFF49A9FF),
                    focusedTextColor = Color(0xFF2D2D2D),
                    unfocusedTextColor = Color(0xFF2D2D2D)
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF49A9FF)
                )
            } else {
                PrimaryButton(
                    text = "Sign up",
                    onClick = {
                        viewModel.signUp(
                            username = username,
                            name = name,
                            password = password,
                            confirmPassword = confirmPassword,
                            onSuccess = onSignUpSuccess
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color(0xFF2D2D2D),
                    fontSize = 16.sp
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Log in here.",
                        color = Color(0xFF49A9FF),
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {
    KushoTheme {
        SignUpScreen()
    }
}
```

Changes: `email` state → `username` state, removed `school` state and its TextField, "Create a Email" → "Create a Username", removed `email` and `school` from `viewModel.signUp()` call.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/auth/signup/SignUpScreen.kt
git commit -m "refactor: replace email/school fields with username in SignUpScreen"
```

---

### Task 8: Update LoginViewModel

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/auth/login/LoginViewModel.kt`

**Step 1: Replace the LoginViewModel class**

Replace the entire file content with:

```kotlin
package com.example.app.ui.feature.auth.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.AppDatabase
import com.example.app.data.SessionManager
import com.example.app.data.entity.User
import com.example.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val userRepository = UserRepository(database.userDao())
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(
        username: String,
        password: String,
        staySignedIn: Boolean = false,
        onSuccess: (User) -> Unit
    ) {
        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter your username"
            )
            return
        }

        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Please enter your password"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = userRepository.login(username, password)) {
                is UserRepository.LoginResult.Success -> {
                    sessionManager.saveUserSession(result.user, staySignedIn)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        loggedInUser = result.user,
                        errorMessage = null
                    )

                    onSuccess(result.user)
                }
                is UserRepository.LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val loggedInUser: User? = null,
    val errorMessage: String? = null
)
```

Changes: `email` param → `username`, "Please enter your email" → "Please enter your username", removed KDoc comments.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/auth/login/LoginViewModel.kt
git commit -m "refactor: rename email to username in LoginViewModel"
```

---

### Task 9: Update LoginScreen

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/auth/login/LoginScreen.kt`

**Step 1: Update the LoginScreen composable**

Replace the entire file content with:

```kotlin
package com.example.app.ui.feature.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.theme.KushoTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var staySignedIn by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Image(
                    painter = painterResource(id = R.drawable.dis_study),
                    contentDescription = null,
                    modifier = Modifier
                        .size(280.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )

                Image(
                    painter = painterResource(id = R.drawable.title_login),
                    contentDescription = "Login to Kusho",
                    modifier = Modifier
                        .width(300.dp)
                        .height(80.dp)
                        .padding(bottom = 32.dp),
                    contentScale = ContentScale.Fit
                )

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF49A9FF),
                        unfocusedIndicatorColor = Color(0xFF49A9FF),
                        focusedTextColor = Color(0xFF2D2D2D),
                        unfocusedTextColor = Color(0xFF2D2D2D)
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF49A9FF)
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF49A9FF),
                        unfocusedIndicatorColor = Color(0xFF49A9FF),
                        focusedTextColor = Color(0xFF2D2D2D),
                        unfocusedTextColor = Color(0xFF2D2D2D)
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = staySignedIn,
                        onCheckedChange = { staySignedIn = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF49A9FF),
                            uncheckedColor = Color(0xFF49A9FF)
                        ),
                        enabled = !uiState.isLoading
                    )
                    Text(
                        text = "Stay Signed in",
                        color = Color(0xFF2D2D2D),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF49A9FF)
                    )
                } else {
                    PrimaryButton(
                        text = "Log in",
                        onClick = {
                            viewModel.login(
                                username = username,
                                password = password,
                                staySignedIn = staySignedIn,
                                onSuccess = { onLoginSuccess() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = Color(0xFF2D2D2D),
                        fontSize = 16.sp
                    )
                    TextButton(
                        onClick = onNavigateToSignUp,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign up here.",
                            color = Color(0xFF49A9FF),
                            fontSize = 16.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    KushoTheme {
        LoginScreen()
    }
}
```

Changes: `email` state → `username` state, placeholder "Email" → "Username", `email = email` → `username = username` in `viewModel.login()` call.

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/app/ui/feature/auth/login/LoginScreen.kt
git commit -m "refactor: replace email with username in LoginScreen"
```

---

### Task 10: Build Verification

**Step 1: Run the Gradle build to verify everything compiles**

```bash
cd /Users/georgette/AndroidStudioProjects/Kusho && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. If there are compilation errors, fix them before proceeding.

**Step 2: Final commit (if any fixes were needed)**

```bash
git add -A
git commit -m "fix: resolve any compilation issues from username rename"
```

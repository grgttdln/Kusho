# Design: Remove Email/School, Use Username for Login

**Date:** 2026-02-19
**Branch:** update/Register-and-Login

## Summary

Replace email-based authentication with username-based authentication. Remove the school field entirely. The username becomes the unique login identifier (no email format required).

## Approach

Full rename (Approach A): rename `email` to `username` across the entire codebase. Destructive database migration (version 14 -> 15).

## Data Layer

### User Entity (`User.kt`)
- Rename `email: String` -> `username: String`
- Remove `school: String`
- Change unique index from `email` to `username`
- Final fields: `id`, `username`, `name`, `passwordHash`, `salt`, `createdAt`

### UserDao (`UserDao.kt`)
- `getUserByEmail(email)` -> `getUserByUsername(username)`
- `isEmailExists(email)` -> `isUsernameExists(username)`
- `updateUserProfile(id, name, school)` -> `updateUserProfile(id, name)` (drop school)
- Update all SQL queries to reference `username` column

### UserRepository (`UserRepository.kt`)
- `signUp(email, name, school, password)` -> `signUp(username, name, password)`
- `login(email, password)` -> `login(username, password)`
- Remove email format validation
- Add username validation: non-blank, trimmed
- Rename `isEmailExists` -> `isUsernameExists`

### AppDatabase (`AppDatabase.kt`)
- Bump version 14 -> 15
- Add `.fallbackToDestructiveMigration()`

### SessionManager (`SessionManager.kt`)
- Replace `email` key with `username` in SharedPreferences
- Remove `school` from stored session data
- Rename accessor methods accordingly

## UI Layer

### SignUpScreen (`SignUpScreen.kt`)
- Replace "Email" field with "Username" field
- Remove "School" field
- Remove email format validation (`Patterns.EMAIL_ADDRESS`)
- Keep: Username, Name, Password, Confirm Password

### SignUpViewModel (`SignUpViewModel.kt`)
- `signUp(email, name, school, password, confirmPassword)` -> `signUp(username, name, password, confirmPassword)`
- Update error messages ("Email already exists" -> "Username already taken")

### LoginScreen (`LoginScreen.kt`)
- Change "Email" label to "Username"
- Remove email keyboard type if set

### LoginViewModel (`LoginViewModel.kt`)
- `login(email, password, staySignedIn)` -> `login(username, password, staySignedIn)`
- Update error messages

## No Changes

- Navigation flow stays the same
- Password hashing (PBKDF2) stays the same
- "Stay Signed In" logic stays the same
- Post-signup onboarding flow stays the same

## Files Affected

1. `data/entity/User.kt`
2. `data/dao/UserDao.kt`
3. `data/repository/UserRepository.kt`
4. `data/AppDatabase.kt`
5. `data/SessionManager.kt`
6. `ui/feature/auth/signup/SignUpScreen.kt`
7. `ui/feature/auth/signup/SignUpViewModel.kt`
8. `ui/feature/auth/login/LoginScreen.kt`
9. `ui/feature/auth/login/LoginViewModel.kt`

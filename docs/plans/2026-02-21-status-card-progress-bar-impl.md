# Status Card Progress Bar Badge Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the simple text badge on LearnModeStatusCard with a fill-bar progress badge showing "Not Started", "X% Progress", or "Completed".

**Architecture:** Add `completionPercentage` parameter to `ActivitySetStatus` and `LearnModeStatusCard`. Replace the badge Box in the card with a layered fill-bar (background + proportional fill + overlaid text). Wire the percentage from `StudentSetProgress.completionPercentage` through `MainNavigationContainer`.

**Tech Stack:** Jetpack Compose, Kotlin, Room (existing)

---

### Task 1: Add `completionPercentage` to ActivitySetStatus

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSetStatusScreen.kt:37-41`

**Step 1: Add the field to the data class**

Change `ActivitySetStatus` from:

```kotlin
data class ActivitySetStatus(
    val setId: Long,
    val title: String,
    val status: String
)
```

To:

```kotlin
data class ActivitySetStatus(
    val setId: Long,
    val title: String,
    val status: String,
    val completionPercentage: Int = 0
)
```

**Step 2: Update the preview data to exercise all three states**

In `LearnModeSetStatusScreenPreview` (around line 165), change the preview list to:

```kotlin
sets = List(25) { index ->
    ActivitySetStatus(
        setId = index.toLong(),
        title = "Set ${index + 1}",
        status = when {
            index % 3 == 0 -> "Completed"
            index % 3 == 1 -> "${25 + index * 5}% Progress"
            else -> "Not Started"
        },
        completionPercentage = when {
            index % 3 == 0 -> 100
            index % 3 == 1 -> 25 + index * 5
            else -> 0
        }
    )
}
```

---

### Task 2: Add `completionPercentage` parameter to LearnModeStatusCard

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/components/learnmode/LearnModeStatusCard.kt`

**Step 1: Add the parameter to the composable signature**

Change the function signature (line 33-38) from:

```kotlin
@Composable
fun LearnModeStatusCard(
    title: String,
    status: String = "Not Started",
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
)
```

To:

```kotlin
@Composable
fun LearnModeStatusCard(
    title: String,
    status: String = "Not Started",
    completionPercentage: Int = 0,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
)
```

**Step 2: Replace the badge Box (lines 71-95) with the fill-bar badge**

Replace the entire block starting from `val isCompleted = status == "Completed"` through the closing `}` of the badge Box (lines 69-95) with:

```kotlin
val isCompleted = status == "Completed"
val fillFraction = (completionPercentage.coerceIn(0, 100)) / 100f

Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .height(28.dp)
        .clip(RoundedCornerShape(12.dp))
        .border(
            width = if (isCompleted && !isSelected) 0.dp else if (!isCompleted && completionPercentage == 0) 2.dp else 0.dp,
            color = Color(0xFFBA9BFF),
            shape = RoundedCornerShape(12.dp)
        )
        .background(
            if (isSelected) Color.White else Color(0xFFF0EAFF),
            RoundedCornerShape(12.dp)
        )
) {
    // Fill layer (proportional)
    if (fillFraction > 0f) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = fillFraction)
                .background(
                    if (isSelected) Color(0xFFBA9BFF) else Color(0xFFAE8EFB),
                    RoundedCornerShape(12.dp)
                )
        )
    }

    // Text layer (centered)
    Text(
        text = status,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = when {
            isSelected && isCompleted -> Color(0xFFAE8EFB)
            isSelected -> Color(0xFFAE8EFB)
            isCompleted -> Color.White
            completionPercentage > 0 -> Color.White
            else -> Color(0xFFBA9BFF)
        },
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center)
    )
}
```

**Step 3: Add necessary imports at the top of the file**

Add `import androidx.compose.foundation.border` if not already present.

**Step 4: Update the previews at the bottom of the file to pass completionPercentage**

Update `LearnModeStatusCardPreview` to pass `completionPercentage = 0` and add a new preview for in-progress state:

```kotlin
@Preview(
    name = "LearnModeStatusCard",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
fun LearnModeStatusCardPreview() {
    MaterialTheme {
        Surface(color = Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LearnModeStatusCard(
                    title = "Short Vowels",
                    status = "Not Started",
                    completionPercentage = 0
                )
            }
        }
    }
}

@Preview(name = "LearnModeStatusCard In Progress", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LearnModeStatusCardInProgressPreview() {
    MaterialTheme {
        Surface(color = Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LearnModeStatusCard(
                    title = "Short Vowels",
                    status = "60% Progress",
                    completionPercentage = 60
                )
            }
        }
    }
}

@Preview(name = "LearnModeStatusCard Selected", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LearnModeStatusCardSelectedPreview() {
    MaterialTheme {
        Surface(color = Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LearnModeStatusCard(
                    title = "Short Vowels",
                    status = "Not Started",
                    completionPercentage = 0,
                    isSelected = true
                )
            }
        }
    }
}
```

---

### Task 3: Pass `completionPercentage` from LearnModeSetStatusScreen to the card

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/learn/learnmode/LearnModeSetStatusScreen.kt:125-128`

**Step 1: Pass the new field to LearnModeStatusCard**

Change the card call (around line 125) from:

```kotlin
LearnModeStatusCard(
    title = set.title,
    status = set.status,
    isSelected = isSelected,
```

To:

```kotlin
LearnModeStatusCard(
    title = set.title,
    status = set.status,
    completionPercentage = set.completionPercentage,
    isSelected = isSelected,
```

---

### Task 4: Wire completionPercentage from StudentSetProgress in MainNavigationContainer

**Files:**
- Modify: `app/src/main/java/com/example/app/ui/feature/home/MainNavigationContainer.kt:627-636` and `755-764`

**Step 1: Update first call site (around line 627)**

Replace the block:

```kotlin
val completedSetIds = remember(progressList) {
    progressList.filter { it.isCompleted }.map { it.setId }.toSet()
}
val activitySetStatuses = sets.map { set ->
    val isCompleted = completedSetIds.contains(set.id)
    com.example.app.ui.feature.learn.learnmode.ActivitySetStatus(
        setId = set.id,
        title = set.title,
        status = if (isCompleted) "Completed" else "Not Started"
    )
}
```

With:

```kotlin
val progressBySetId = remember(progressList) {
    progressList.associateBy { it.setId }
}
val activitySetStatuses = sets.map { set ->
    val progress = progressBySetId[set.id]
    val percentage = progress?.completionPercentage ?: 0
    com.example.app.ui.feature.learn.learnmode.ActivitySetStatus(
        setId = set.id,
        title = set.title,
        status = when {
            progress?.isCompleted == true || percentage == 100 -> "Completed"
            percentage > 0 -> "$percentage% Progress"
            else -> "Not Started"
        },
        completionPercentage = percentage
    )
}
```

**Step 2: Update second call site (around line 755)**

Apply the exact same replacement to the second block (around lines 755-764). Replace:

```kotlin
val completedSetIds = remember(progressList) {
    progressList.filter { it.isCompleted }.map { it.setId }.toSet()
}
val activitySetStatuses = sets.map { set ->
    val isCompleted = completedSetIds.contains(set.id)
    com.example.app.ui.feature.learn.learnmode.ActivitySetStatus(
        setId = set.id,
        title = set.title,
        status = if (isCompleted) "Completed" else "Not Started"
    )
}
```

With:

```kotlin
val progressBySetId = remember(progressList) {
    progressList.associateBy { it.setId }
}
val activitySetStatuses = sets.map { set ->
    val progress = progressBySetId[set.id]
    val percentage = progress?.completionPercentage ?: 0
    com.example.app.ui.feature.learn.learnmode.ActivitySetStatus(
        setId = set.id,
        title = set.title,
        status = when {
            progress?.isCompleted == true || percentage == 100 -> "Completed"
            percentage > 0 -> "$percentage% Progress"
            else -> "Not Started"
        },
        completionPercentage = percentage
    )
}
```

---

### Task 5: Verify build compiles

**Step 1: Build the project**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

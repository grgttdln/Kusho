package com.example.app.ui.feature.learn.activities

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.app.ui.feature.learn.ConfirmationScreen as ReusableConfirmationScreen

/**
 * Activity-specific confirmation screen.
 * Delegates to the reusable ConfirmationScreen with "Activity Created!" message.
 */
@Composable
fun ConfirmationScreen(
    activityTitle: String = "",
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ReusableConfirmationScreen(
        title = "Activity Created!",
        subtitle = activityTitle,
        onNavigate = { screen ->
            // Override navigation for activities - go to YourActivitiesScreen (screen 6)
            onNavigate(if (screen == 7) 6 else screen)
        },
        modifier = modifier
    )
}

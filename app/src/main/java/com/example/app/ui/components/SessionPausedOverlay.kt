package com.example.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Non-blocking overlay shown when the watch exits the mode screen mid-session.
 *
 * Displays a semi-transparent scrim over the session screen with a centered card
 * indicating the session is paused. The overlay prevents interaction with the
 * session content underneath.
 *
 * Uses a blue color scheme consistent with [WatchDisconnectedDialog].
 *
 * @param watchOnScreen Whether the watch is currently on the mode screen.
 *   - false: Shows a spinner with "Waiting for watch..." text.
 *   - true: Shows a green checkmark with "Watch ready" text, and enables the Continue button.
 * @param onContinue Called when the teacher taps Continue (only enabled when [watchOnScreen] is true).
 * @param onEndSession Called when the teacher taps End Session to bail out.
 */
@Composable
fun SessionPausedOverlay(
    watchOnScreen: Boolean,
    onContinue: () -> Unit,
    onEndSession: () -> Unit
) {
    // Semi-transparent dark scrim that fills the entire screen and consumes clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consume clicks to prevent interaction with session screen underneath */ },
        contentAlignment = Alignment.Center
    ) {
        // Centered card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
            ) {
                // Blue header bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color(0xFF49A9FF))
                )

                // White content section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Session Paused",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B0B0B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtitle
                    Text(
                        text = "The watch has exited the current mode",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status indicator
                    if (!watchOnScreen) {
                        // Waiting state: spinner + waiting text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF49A9FF)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Waiting for watch...",
                                fontSize = 14.sp,
                                color = Color(0xFF888888),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Ready state: checkmark + ready text
                        Text(
                            text = "\u2713 Watch ready",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Continue button (primary, enabled only when watch is on screen)
                    Button(
                        onClick = onContinue,
                        enabled = watchOnScreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF49A9FF),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (watchOnScreen) Color.White else Color(0xFF9E9E9E)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // End Session button (secondary)
                    Button(
                        onClick = onEndSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD6EDFF)
                        )
                    ) {
                        Text(
                            text = "End Session",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF49A9FF)
                        )
                    }
                }
            }
        }
    }
}

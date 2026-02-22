package com.example.app.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val BlueBannerColor = Color(0xFF3FA9F8)
private val LightBlueColor = Color(0xFFD6EAFB)

/**
 * Shared resume session dialog used by both Learn Mode and Tutorial Mode.
 * Shows a card with blue banner, mascot overlapping the top, progress info,
 * and side-by-side Resume/Restart buttons.
 */
@Composable
fun ResumeSessionDialog(
    mascotDrawable: Int,
    studentName: String,
    completedCount: Int,
    totalCount: Int,
    unitLabel: String,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Non-dismissable — must choose */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Block clicks through overlay
                ),
            contentAlignment = Alignment.Center
        ) {
            // Card + overlapping mascot
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // White card body (offset down to leave room for mascot overlap)
                Column(
                    modifier = Modifier
                        .padding(top = 80.dp) // Room for mascot overlap
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Blue banner area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(BlueBannerColor)
                    )

                    Spacer(Modifier.height(16.dp))

                    // "Welcome Back" text
                    Text(
                        text = "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Progress text with bold fraction
                    Text(
                        text = buildAnnotatedString {
                            append("$studentName has completed ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Black)) {
                                append("$completedCount/$totalCount")
                            }
                            append(" $unitLabel.")
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Would you like to continue where you left off?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // Side-by-side buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Resume button (filled blue)
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueBannerColor
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = "Resume",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Restart button (light blue fill)
                        Button(
                            onClick = onRestart,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightBlueColor
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = "Restart",
                                color = BlueBannerColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // Mascot (overlapping the top of the card)
                Image(
                    painter = painterResource(id = mascotDrawable),
                    contentDescription = "Resume session mascot",
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .size(160.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

package com.example.app.ui.components.tutorial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.R

/**
 * A reusable dialog component for selecting letter type (Capital or Small).
 * Displays a modal with "Which one should we practice?" question and two buttons.
 *
 * @param onDismiss Callback when the dialog is dismissed
 * @param onSelectType Callback when a letter type is selected, returns "capital" or "small"
 * @param modifier Optional modifier for the dialog
 */
@Composable
fun LetterTypeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Main card content (positioned below the mascot)
                Column(
                    modifier = Modifier
                        .padding(top = 80.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    // Yellow header section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(Color(0xFFEDBB00))
                    )

                    // White content section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Question text
                        Text(
                            text = "Which one should we practice?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B0B0B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Capital button
                            Button(
                                onClick = { onSelectType("capital") },
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(2.dp, Color(0xFFEDBB00)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_uppercase),
                                        contentDescription = "Capital letters",
                                        modifier = Modifier.size(180.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "Uppercase",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0B0B0B)
                                    )
                                }
                            }

                            // Small button
                            Button(
                                onClick = { onSelectType("small") },
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(2.dp, Color(0xFFEDBB00)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_lowercase),
                                        contentDescription = "Small letters",
                                        modifier = Modifier.size(180.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "Lowercase",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0B0B0B)
                                    )
                                }
                            }
                        }
                    }
                }

                // Question mascot image overlapping the top
                Image(
                    painter = painterResource(id = R.drawable.dis_question),
                    contentDescription = "Question",
                    modifier = Modifier
                        .size(160.dp)
                        .offset(y = 0.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

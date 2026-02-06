package com.example.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.R

private val PurpleColor = Color(0xFFAE8EFB)

/**
 * Data class representing a selectable chip option
 */
data class ChipOption(
    val label: String,
    val color: Color,
    val isSelected: Boolean = false
)

/**
 * Selectable chip with colored dot indicator
 */
@Composable
fun SelectableChip(
    label: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PurpleColor else Color(0xFFE0E0E0)
    val backgroundColor = if (isSelected) PurpleColor.copy(alpha = 0.1f) else Color.White

    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Colored dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.Black
            )
        }
    }
}

/**
 * Learner Profile Annotation Dialog
 * Shows when the annotate button is clicked to allow teachers to record observations
 * Features scrollable content and smooth slide-up/down animation
 */
@Composable
fun LearnerProfileAnnotationDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onAddNote: (
        levelOfProgress: String?,
        strengthsObserved: List<String>,
        strengthsNote: String,
        challenges: List<String>,
        challengesNote: String
    ) -> Unit
) {
    // Animation state for slide-up/down effect
    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    val slideOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible && !isClosing) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 300,
            easing = if (isClosing)
                androidx.compose.animation.core.FastOutLinearInEasing
            else
                androidx.compose.animation.core.FastOutSlowInEasing
        ),
        finishedListener = {
            if (isClosing) {
                onDismiss()
            }
        },
        label = "slideOffset"
    )
    val backgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible && !isClosing) 0.5f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 300
        ),
        label = "backgroundAlpha"
    )

    // Function to trigger close animation
    val animateClose: () -> Unit = {
        isClosing = true
    }

    // Trigger animation when dialog appears
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Level of Progress options
    val levelOptions = listOf(
        ChipOption("Beginning", Color(0xFFFF6B6B)),
        ChipOption("Developing", Color(0xFFFFB800)),
        ChipOption("Proficient", Color(0xFF4CAF50)),
        ChipOption("Advanced", Color(0xFF2196F3))
    )
    var selectedLevel by remember { mutableStateOf<String?>(null) }

    // Strengths Observed options
    val strengthOptions = listOf("Recognition", "Fluency", "Formation")
    var selectedStrengths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var strengthsNote by remember { mutableStateOf("") }
    var showStrengthsNoteField by remember { mutableStateOf(false) }

    // Challenges options
    val challengeOptions = listOf("Recognition", "Fluency", "Formation")
    var selectedChallenges by remember { mutableStateOf<Set<String>>(emptySet()) }
    var challengesNote by remember { mutableStateOf("") }
    var showChallengesNoteField by remember { mutableStateOf(false) }

    // Scroll state for content
    val scrollState = androidx.compose.foundation.rememberScrollState()

    Dialog(
        onDismissRequest = animateClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { animateClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .offset(y = (slideOffset * 600).dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Prevent click through */ },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Fixed Header with icon and student name
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)
                    ) {
                        // Drag handle indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.LightGray)
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_annotate),
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Learner Profile: $studentName",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp)
                    ) {
                        // Level of Progress section
                        Text(
                            text = "Level of Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))

                        // Level chips - first row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            levelOptions.take(3).forEach { option ->
                                SelectableChip(
                                    label = option.label,
                                    dotColor = option.color,
                                    isSelected = selectedLevel == option.label,
                                    onClick = {
                                        selectedLevel = if (selectedLevel == option.label) null else option.label
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Level chips - second row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            levelOptions.drop(3).forEach { option ->
                                SelectableChip(
                                    label = option.label,
                                    dotColor = option.color,
                                    isSelected = selectedLevel == option.label,
                                    onClick = {
                                        selectedLevel = if (selectedLevel == option.label) null else option.label
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Strengths Observed section
                        Text(
                            text = "Strengths Observed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            strengthOptions.forEach { option ->
                                SelectableChip(
                                    label = option,
                                    dotColor = Color(0xFF2196F3),
                                    isSelected = option in selectedStrengths,
                                    onClick = {
                                        selectedStrengths = if (option in selectedStrengths) {
                                            selectedStrengths - option
                                        } else {
                                            selectedStrengths + option
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Add Note for Strengths
                        if (showStrengthsNoteField) {
                            androidx.compose.material3.OutlinedTextField(
                                value = strengthsNote,
                                onValueChange = { strengthsNote = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add a note...", color = Color.Gray) },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleColor,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                minLines = 2,
                                maxLines = 4
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clickable { showStrengthsNoteField = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Add Note",
                                    fontSize = 14.sp,
                                    color = PurpleColor
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Challenges section
                        Text(
                            text = "Challenges",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            challengeOptions.forEach { option ->
                                SelectableChip(
                                    label = option,
                                    dotColor = Color(0xFF2196F3),
                                    isSelected = option in selectedChallenges,
                                    onClick = {
                                        selectedChallenges = if (option in selectedChallenges) {
                                            selectedChallenges - option
                                        } else {
                                            selectedChallenges + option
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Add Note for Challenges
                        if (showChallengesNoteField) {
                            androidx.compose.material3.OutlinedTextField(
                                value = challengesNote,
                                onValueChange = { challengesNote = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add a note...", color = Color.Gray) },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleColor,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                minLines = 2,
                                maxLines = 4
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clickable { showChallengesNoteField = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Add Note",
                                    fontSize = 14.sp,
                                    color = PurpleColor
                                )
                            }
                        }

                        // Bottom padding for scroll content
                        Spacer(Modifier.height(24.dp))
                    }

                    // Fixed Add Note button at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(24.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                onAddNote(
                                    selectedLevel,
                                    selectedStrengths.toList(),
                                    strengthsNote,
                                    selectedChallenges.toList(),
                                    challengesNote
                                )
                                animateClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A90D9)
                            )
                        ) {
                            Text(
                                text = "Add Note",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}


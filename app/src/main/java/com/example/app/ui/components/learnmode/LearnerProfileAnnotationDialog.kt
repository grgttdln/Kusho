package com.example.app.ui.components.learnmode

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.app.R

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
    onClick: () -> Unit,
    accentColor: Color = Color(0xFFAE8EFB)
) {
    val borderColor = if (isSelected) accentColor else Color(0xFFE0E0E0)
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.1f) else Color.White

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
 *
 * @param studentName The name of the student being annotated
 * @param existingData Optional existing annotation data to pre-populate the dialog
 * @param onDismiss Callback when the dialog is dismissed without saving
 * @param onAddNote Callback when the user saves the annotation
 */
@Composable
fun LearnerProfileAnnotationDialog(
    studentName: String,
    existingData: AnnotationData = AnnotationData.empty(),
    onDismiss: () -> Unit,
    onAddNote: (
        levelOfProgress: String?,
        strengthsObserved: List<String>,
        strengthsNote: String,
        challenges: List<String>,
        challengesNote: String
    ) -> Unit,
    accentColor: Color = Color(0xFFAE8EFB), // Primary color for borders, text, chips
    buttonColor: Color = Color(0xFFAE8EFB) // Button background color
) {
    // Animation state for slide-up/down effect
    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    val slideOffset by animateFloatAsState(
        targetValue = if (isVisible && !isClosing) 0f else 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = if (isClosing)
                FastOutLinearInEasing
            else
                FastOutSlowInEasing
        ),
        finishedListener = {
            if (isClosing) {
                onDismiss()
            }
        },
        label = "slideOffset"
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isVisible && !isClosing) 0.5f else 0f,
        animationSpec = tween(
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
    // Initialize with existing data
    var selectedLevel by remember { mutableStateOf(existingData.levelOfProgress) }

    // Strengths Observed options
    val strengthOptions = listOf("Recognition", "Fluency", "Formation")
    var selectedStrengths by remember { mutableStateOf(existingData.strengthsObserved) }
    var strengthsNote by remember { mutableStateOf(existingData.strengthsNote) }
    var showStrengthsNoteField by remember { mutableStateOf(existingData.strengthsNote.isNotBlank()) }

    // Challenges options
    val challengeOptions = listOf("Recognition", "Fluency", "Formation")
    var selectedChallenges by remember { mutableStateOf(existingData.challenges) }
    var challengesNote by remember { mutableStateOf(existingData.challengesNote) }
    var showChallengesNoteField by remember { mutableStateOf(existingData.challengesNote.isNotBlank()) }

    // Scroll state for content
    val scrollState = rememberScrollState()

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
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(accentColor)
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
                                    },
                                    accentColor = accentColor
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
                                    },
                                    accentColor = accentColor
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
                                    },
                                    accentColor = accentColor
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Add Note for Strengths
                        if (showStrengthsNoteField) {
                            OutlinedTextField(
                                value = strengthsNote,
                                onValueChange = { strengthsNote = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add a note...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
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
                                    color = accentColor
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
                                    },
                                    accentColor = accentColor
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Add Note for Challenges
                        if (showChallengesNoteField) {
                            OutlinedTextField(
                                value = challengesNote,
                                onValueChange = { challengesNote = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add a note...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
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
                                    color = accentColor
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
                        Button(
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor
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


package com.example.app.ui.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.media.MediaPlayer
import com.example.app.R

private val CorrectColor = Color(0xFFCCDB00)
private val IncorrectColor = Color(0xFFFF6B6B)

@Composable
fun ProgressCheckDialog(
    isCorrect: Boolean,
    studentName: String,
    targetLetter: String,
    targetCase: String,
    predictedLetter: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    
    // Play audio when dialog appears
    DisposableEffect(isCorrect) {
        try {
            val audioResId = if (isCorrect) R.raw.correct else R.raw.wrong
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$audioResId"))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        onDispose {
            mediaPlayer.release()
        }
    }
    // Extract first name only for more friendly tone
    val firstName = studentName.split(" ").firstOrNull()?.takeIf { it.isNotEmpty() } ?: ""
    
    // Define similar shape letters (same as watch side)
    val similarShapeLetters = setOf('C', 'K', 'O', 'P', 'S', 'V', 'W', 'X', 'Z')
    
    // Check if there's a case mismatch for similar letters
    val targetUppercase = targetLetter.uppercase()
    val isSimilarShape = targetUppercase.firstOrNull() in similarShapeLetters
    val expectedCase = when (targetCase.lowercase()) {
        "small", "lowercase" -> targetLetter.lowercase()
        else -> targetLetter.uppercase()
    }
    val hasCaseMismatch = isCorrect && isSimilarShape && 
                         predictedLetter.isNotEmpty() && 
                         !predictedLetter.equals(expectedCase, ignoreCase = false)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Full-screen dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mascot Image
                Image(
                    painter = painterResource(
                        id = if (isCorrect) R.drawable.dis_mobile_correct else R.drawable.dis_mobile_incorrect
                    ),
                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Title with first name only
                Text(
                    text = if (isCorrect) {
                        "Great Job${if (firstName.isNotEmpty()) ", $firstName" else ""}!"
                    } else {
                        "Not quite${if (firstName.isNotEmpty()) ", $firstName" else ""}!"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) CorrectColor else IncorrectColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Body text with optional case mismatch disclaimer
                if (isCorrect && hasCaseMismatch) {
                    Text(
                        text = "You're doing super!\nKeep up the amazing work!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Psst... you wrote ${if (predictedLetter.first().isUpperCase()) "uppercase" else "lowercase"} ${predictedLetter.uppercase()}, " +
                              "but we're practicing ${if (targetCase.lowercase() in listOf("small", "lowercase")) "lowercase" else "uppercase"} letters! " +
                              "They look similar, so that's still great!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                } else if (isCorrect) {
                    Text(
                        text = "You're doing super!\nKeep up the amazing work!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                } else {
                    Text(
                        text = "Let's give it another go!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCheckDialogCorrectPreview() {
    ProgressCheckDialog(
        isCorrect = true,
        studentName = "John Doe",
        targetLetter = "A",
        targetCase = "capital",
        predictedLetter = "A",
        onDismiss = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ProgressCheckDialogIncorrectPreview() {
    ProgressCheckDialog(
        isCorrect = false,
        studentName = "Jane Smith",
        targetLetter = "B",
        targetCase = "small",
        predictedLetter = "D",
        onDismiss = {}
    )
}

package com.example.kusho.presentation.learn

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.kusho.presentation.components.CircularModeBorder
import com.example.kusho.presentation.service.PhoneCommunicationManager
import com.example.kusho.presentation.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Learn Mode screen - displays title with swipe-to-skip gesture
 * Swipe Left: Skip to next word (sends command to phone)
 * Swipe Right: Native Wear OS back gesture (preserved)
 */
@Composable
fun LearnModeScreen() {
    val viewModel: LearnModeViewModel = viewModel(
        factory = LearnModeViewModelFactory()
    )

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val phoneCommunicationManager = remember { PhoneCommunicationManager(context) }
    val isPhoneInLearnMode by phoneCommunicationManager.isPhoneInLearnMode.collectAsState()
    
    // Debouncing state for skip gesture
    var lastSkipTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    CircularModeBorder(borderColor = AppColors.LearnModeColor) {
        Scaffold {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isPhoneInLearnMode) {
                    // Waiting state - phone hasn't started Learn Mode session yet
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Waiting...",
                            color = AppColors.LearnModeColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Start Learn Mode\non your phone",
                            color = AppColors.LearnModeColor.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    // Active Learn Mode - enable swipe to skip
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    // Only handle left swipe for skip with debouncing
                                    if (dragAmount < -50f) {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastSkipTime >= 500) { // 500ms debounce
                                            change.consume()
                                            lastSkipTime = currentTime
                                            scope.launch {
                                                phoneCommunicationManager.sendSkipCommand()
                                            }
                                        }
                                    }
                                    // Don't consume right swipe - let system handle back navigation
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.title,
                                color = AppColors.LearnModeColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Swipe left to skip",
                                color = AppColors.LearnModeColor.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


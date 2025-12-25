package com.example.kusho.presentation.tutorial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.kusho.presentation.components.CircularModeBorder
import com.example.kusho.presentation.theme.AppColors

/**
 * Tutorial Mode screen - displays title only for now
 */
@Composable
fun TutorialModeScreen() {
    val viewModel: TutorialModeViewModel = viewModel(
        factory = TutorialModeViewModelFactory()
    )

    val uiState by viewModel.uiState.collectAsState()

    CircularModeBorder(borderColor = AppColors.TutorialModeColor) {
        Scaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.title,
                    color = AppColors.TutorialModeColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


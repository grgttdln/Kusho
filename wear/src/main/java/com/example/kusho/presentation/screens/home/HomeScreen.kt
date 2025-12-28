package com.example.kusho.presentation.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kusho.R
import com.example.kusho.presentation.components.drawModeArc
import com.example.kusho.presentation.navigation.NavigationRoutes
import com.example.kusho.presentation.theme.AppColors
import com.example.kusho.presentation.utils.ArcConstants
import com.example.kusho.presentation.utils.ArcHelper

/**
 * Home screen displaying the Kusho logo with three mode arcs
 */
@Composable
fun HomeScreen(navController: NavHostController) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = size.width / ArcConstants.RADIUS_FACTOR

                    // Calculate which arc was tapped
                    val tappedMode = ArcHelper.getTappedMode(
                        tapX = offset.x,
                        tapY = offset.y,
                        centerX = centerX,
                        centerY = centerY,
                        radius = radius
                    )

                    // Navigate to the appropriate mode screen
                    when (tappedMode) {
                        "practice" -> navController.navigate(NavigationRoutes.PRACTICE_MODE)
                        "tutorial" -> navController.navigate(NavigationRoutes.TUTORIAL_MODE)
                        "learn" -> navController.navigate(NavigationRoutes.LEARN_MODE)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Center logo
        Image(
            painter = painterResource(id = R.drawable.ic_kusho),
            contentDescription = "Kusho Logo",
            modifier = Modifier.size(ArcConstants.LOGO_SIZE_DP.dp),
            contentScale = ContentScale.Fit
        )

        // Draw mode arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = size.width / ArcConstants.RADIUS_FACTOR

            // Draw all three mode arcs
            drawModeArc(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                centerAngle = ArcConstants.PRACTICE_MODE_ANGLE,
                text = "Practice Mode",
                color = AppColors.PracticeModeColor,
                density = density
            )

            drawModeArc(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                centerAngle = ArcConstants.TUTORIAL_MODE_ANGLE,
                text = "Tutorial Mode",
                color = AppColors.TutorialModeColor,
                density = density
            )

            drawModeArc(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                centerAngle = ArcConstants.LEARN_MODE_ANGLE,
                text = "Learn Mode",
                color = AppColors.LearnModeColor,
                density = density
            )
        }
    }
}

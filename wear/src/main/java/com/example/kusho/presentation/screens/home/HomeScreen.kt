package com.example.kusho.presentation.screens.home

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.example.kusho.R
import com.example.kusho.presentation.service.ConnectionMonitor
import com.example.kusho.presentation.service.DisconnectionReason
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
    val context = LocalContext.current
    
    // Check if pairing was skipped
    val prefs = context.getSharedPreferences("kusho_prefs", android.content.Context.MODE_PRIVATE)
    val isSkipped = prefs.getBoolean("is_skipped", false)
    val isPaired = prefs.getBoolean("is_paired", false)
    
    // Monitor connection state if paired and not skipped
    val connectionMonitor = remember { ConnectionMonitor.getInstance(context) }
    val isConnected by connectionMonitor.isConnected.collectAsState()
    
    // Start monitoring when entering home screen if paired
    LaunchedEffect(isPaired, isSkipped) {
        if (isPaired && !isSkipped) {
            Log.d("HomeScreen", "Starting connection monitoring")
            connectionMonitor.startMonitoring()
        }
    }
    
    // Navigate to pairing screen immediately when connection fails
    LaunchedEffect(isConnected) {
        if (!isConnected && isPaired && !isSkipped) {
            // Connection lost - navigate to pairing screen
            Log.w("HomeScreen", "Connection lost - navigating to pairing screen")
            prefs.edit().putBoolean("is_paired", false).apply()
            connectionMonitor.stopMonitoring()
            connectionMonitor.resetFailureCounter()
            navController.navigate(NavigationRoutes.PAIRING) {
                popUpTo(NavigationRoutes.HOME) { inclusive = true }
            }
        }
    }

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
                        "tutorial" -> {
                            // Only allow if not skipped
                            if (!isSkipped) {
                                navController.navigate(NavigationRoutes.TUTORIAL_MODE)
                            }
                        }
                        "learn" -> {
                            // Only allow if not skipped
                            if (!isSkipped) {
                                navController.navigate(NavigationRoutes.LEARN_MODE)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Show pairing button at top if skipped
        if (isSkipped) {
            PairingIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                onClick = {
                    // Clear skipped status and navigate to pairing
                    prefs.edit().putBoolean("is_skipped", false).apply()
                    navController.navigate(NavigationRoutes.PAIRING) {
                        popUpTo(NavigationRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }
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
                color = if (isSkipped) Color(0xFF555555) else AppColors.TutorialModeColor,
                density = density
            )

            drawModeArc(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                centerAngle = ArcConstants.LEARN_MODE_ANGLE,
                text = "Learn Mode",
                color = if (isSkipped) Color(0xFF555555) else AppColors.LearnModeColor,
                density = density
            )
        }

    }
}

/**
 * Pairing indicator shown when user skipped pairing
 */
@Composable
private fun PairingIndicator(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(top = 8.dp)
            .size(44.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF4A90E2)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "⟳",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Connection Lost Dialog - Blocks user until they retry or skip
 * Shows immediately when connection fails (any layer)
 */
@Composable
private fun ConnectionLostDialog(
    reason: DisconnectionReason,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    // Full-screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error icon
            Image(
                painter = painterResource(id = R.drawable.dis_remove),
                contentDescription = "Connection lost",
                modifier = Modifier
                    .size(60.dp)
                    .padding(bottom = 8.dp),
                contentScale = ContentScale.Fit
            )
            
            // Title
            Text(
                text = "Connection Lost",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Reason message
            val reasonText = when (reason) {
                DisconnectionReason.BLUETOOTH_DISCONNECTED -> "Phone disconnected"
                DisconnectionReason.APP_NOT_RESPONDING -> "Kusho app not running"
                DisconnectionReason.NONE -> "Unknown error"
            }
            
            Text(
                text = reasonText,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Retry button
                Button(
                    onClick = onRetry,
                    modifier = Modifier.width(75.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4A90E2)
                    )
                ) {
                    Text(
                        text = "Retry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Skip button
                Button(
                    onClick = onSkip,
                    modifier = Modifier.width(75.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF666666)
                    )
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Skip = Practice only",
                fontSize = 9.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )
        }
    }
}

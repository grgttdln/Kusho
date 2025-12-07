package com.example.kusho.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.kusho.presentation.navigation.AppNavigation
import com.example.kusho.presentation.screens.splash.CustomSplashScreen

/**
 * Main Activity - Entry point for the Kusho Wear OS app
 *
 * Responsibilities:
 * - Activity lifecycle management
 * - Window configuration
 * - App initialization
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set black window background to prevent system icon flash
        window.setBackgroundDrawableResource(android.R.color.black)

        setContent {
            KushoWearApp()
        }
    }
}

/**
 * Main composable for the Kusho Wear app
 *
 * Handles splash screen display and navigation initialization
 */
@Composable
fun KushoWearApp() {
    var showSplash by remember { mutableStateOf(true) }

    MaterialTheme {
        if (showSplash) {
            CustomSplashScreen(onTimeout = { showSplash = false })
        } else {
            AppNavigation()
        }
    }
}

/**
 * Preview for the Kusho Wear app
 */
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    KushoWearApp()
}


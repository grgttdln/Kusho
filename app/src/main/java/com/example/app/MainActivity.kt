package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.app.ui.screens.OnboardingScreen
import com.example.app.ui.theme.KushoTheme
import com.example.kusho.common.MessageService
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    private lateinit var messageService: MessageService
    private lateinit var nodeClient: NodeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        messageService = MessageService(this)
        nodeClient = Wearable.getNodeClient(this)

        setContent {
            KushoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White
                ) { innerPadding ->
                    OnboardingScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onStartLearning = {
                            // TODO: Navigate to learning screen or handle start learning action
                        }
                    )
                }
            }
        }
    }
}

package com.example.kusho.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.kusho.common.MessageService
import com.example.kusho.presentation.theme.KushoTheme
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private lateinit var messageService: MessageService
    private lateinit var nodeClient: NodeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        messageService = MessageService(this)
        nodeClient = Wearable.getNodeClient(this)

        setContent {
            WearApp(messageService = messageService, nodeClient = nodeClient)
        }
    }
}

@Composable
fun WearApp(messageService: MessageService, nodeClient: NodeClient) {
    var messageResult by remember { mutableStateOf<String?>(null) }
    val receivedMessage by messageService.message.collectAsState()
    val scope = rememberCoroutineScope()

    KushoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    Log.d("WearApp", "Send button clicked.")
                    scope.launch {
                        try {
                            val nodes = nodeClient.connectedNodes.await()
                            if (nodes.isEmpty()) {
                                Log.d("WearApp", "No connected nodes found.")
                                messageResult = "No phone connected"
                            } else {
                                for (node in nodes) {
                                    Log.d("WearApp", "Sending message to: ${node.displayName}")
                                    messageService.sendMessage(node, "/message_path", "Hello from watch")
                                }
                                messageResult = "Message Sent!"
                            }
                        } catch (e: Exception) {
                            Log.e("WearApp", "Failed to get connected nodes.", e)
                            messageResult = "Error sending message"
                        }
                    }
                }) {
                    Text(text = "Send Message to Phone")
                }

                (receivedMessage ?: messageResult)?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    WearApp(
        messageService = MessageService(context),
        nodeClient = Wearable.getNodeClient(context)
    )
}

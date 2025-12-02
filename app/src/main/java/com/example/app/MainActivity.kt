package com.example.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.KushoTheme
import com.example.kusho.common.MessageService
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhoneApp(
                        modifier = Modifier.padding(innerPadding),
                        messageService = messageService,
                        nodeClient = nodeClient
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneApp(modifier: Modifier = Modifier, messageService: MessageService, nodeClient: NodeClient) {
    var messageResult by remember { mutableStateOf<String?>(null) }
    val receivedMessage by messageService.message.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                Log.d("App", "Send button clicked.")
                scope.launch {
                    try {
                        val nodes = nodeClient.connectedNodes.await()
                        if (nodes.isEmpty()) {
                            Log.d("App", "No connected nodes found.")
                            messageResult = "No watch connected"
                        } else {
                            for (node in nodes) {
                                Log.d("App", "Sending message to: ${node.displayName}")
                                messageService.sendMessage(node, "/message_path", "Hello from phone")
                            }
                            messageResult = "Message Sent!"
                        }
                    } catch (e: Exception) {
                        Log.e("App", "Failed to get connected nodes.", e)
                        messageResult = "Error sending message"
                    }
                }
            }) {
                Text(text = "Send Message to Watch")
            }

            (receivedMessage ?: messageResult)?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KushoTheme {
        PhoneApp(
            messageService = MessageService(androidx.compose.ui.platform.LocalContext.current),
            nodeClient = Wearable.getNodeClient(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}

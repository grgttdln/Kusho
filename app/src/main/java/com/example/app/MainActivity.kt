package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Hello World")
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

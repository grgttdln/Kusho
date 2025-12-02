package com.example.kusho.common

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MessageService(context: Context) : MessageClient.OnMessageReceivedListener {

    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        messageClient.addListener(this)
    }

    fun sendMessage(node: Node, path: String, message: String) {
        messageClient.sendMessage(node.id, path, message.toByteArray())
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        _message.value = String(messageEvent.data)
    }
}

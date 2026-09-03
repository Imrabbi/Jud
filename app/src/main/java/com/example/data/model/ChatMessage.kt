package com.example.data.model

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

enum class ActionStatus {
    SUCCESS,
    FAILED,
    PENDING,
    INFO
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val status: ActionStatus = ActionStatus.INFO,
    val statusMessage: String? = null
)

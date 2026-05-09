package com.lm.chatbot.model

data class ChatMessage(
    val id: Int,
    val content: String,
    val role: ChatRole
) {
    val isFromUser: Boolean
        get() = role == ChatRole.User
}

enum class ChatRole(val apiValue: String) {
    User("user"),
    Assistant("assistant")
}

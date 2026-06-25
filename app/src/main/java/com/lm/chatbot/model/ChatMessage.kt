package com.lm.chatbot.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: Int,
    val content: String,
    val role: ChatRole,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isFromUser: Boolean
        get() = role == ChatRole.User
    
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

enum class ChatRole(val apiValue: String) {
    User("user"),
    Assistant("assistant")
}

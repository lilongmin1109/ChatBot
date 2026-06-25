package com.lm.chatbot.data

import com.lm.chatbot.model.ChatMessage
import com.lm.chatbot.model.ChatRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatStorage {
    private const val FILE_NAME = "chat_history.json"

    fun saveMessages(dir: File, messages: List<ChatMessage>) {
        val json = JSONArray().apply {
            messages.forEach { msg ->
                put(
                    JSONObject()
                        .put("id", msg.id)
                        .put("content", msg.content)
                        .put("role", msg.role.apiValue)
                        .put("timestamp", msg.timestamp)
                )
            }
        }
        File(dir, FILE_NAME).writeText(json.toString())
    }

    fun loadMessages(dir: File): List<ChatMessage>? {
        val file = File(dir, FILE_NAME)
        if (!file.exists()) return null

        val array = JSONArray(file.readText())
        val messages = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ChatMessage(
                id = obj.getInt("id"),
                content = obj.getString("content"),
                role = if (obj.getString("role") == "user") ChatRole.User else ChatRole.Assistant,
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
        return messages.ifEmpty { null }
    }

    fun clearMessages(dir: File) {
        val file = File(dir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}

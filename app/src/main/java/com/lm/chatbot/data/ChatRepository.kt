package com.lm.chatbot.data

import com.lm.chatbot.BuildConfig
import com.lm.chatbot.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ChatRepository(
    private val apiClient: DeepSeekApiClient = DeepSeekApiClient(BuildConfig.DEEPSEEK_API_KEY),
    private val filesDir: File
) {
    suspend fun sendMessages(messages: List<ChatMessage>): String {
        return withContext(Dispatchers.IO) {
            apiClient.createChatCompletion(messages)
        }
    }

    suspend fun sendMessagesStream(
        messages: List<ChatMessage>,
        webSearchEnabled: Boolean = false,
        onToken: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            if (webSearchEnabled) {
                apiClient.createWebSearchStream(messages, onToken)
            } else {
                apiClient.createChatCompletionStream(messages, onToken)
            }
        }
    }

    suspend fun loadMessages(): List<ChatMessage>? {
        return withContext(Dispatchers.IO) {
            ChatStorage.loadMessages(filesDir)
        }
    }

    suspend fun saveMessages(messages: List<ChatMessage>) {
        withContext(Dispatchers.IO) {
            ChatStorage.saveMessages(filesDir, messages)
        }
    }
}

package com.lm.chatbot.data

import com.lm.chatbot.BuildConfig
import com.lm.chatbot.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val apiClient: DeepSeekApiClient = DeepSeekApiClient(BuildConfig.DEEPSEEK_API_KEY)
) {
    suspend fun sendMessages(messages: List<ChatMessage>): String {
        return withContext(Dispatchers.IO) {
            apiClient.createChatCompletion(messages)
        }
    }
}

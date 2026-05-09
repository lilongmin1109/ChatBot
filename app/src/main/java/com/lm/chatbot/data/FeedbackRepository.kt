package com.lm.chatbot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedbackRepository(
    private val apiClient: FeedbackApiClient = FeedbackApiClient()
) {
    suspend fun submitFeedback(content: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                apiClient.submitFeedback(content)
            }
        }
    }
}

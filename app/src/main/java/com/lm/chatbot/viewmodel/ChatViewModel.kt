package com.lm.chatbot.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lm.chatbot.data.ChatRepository
import com.lm.chatbot.model.ChatMessage
import com.lm.chatbot.model.ChatRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_SAVED_MESSAGES = 10

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(1, "你好，我是智能聊天助手。", ChatRole.Assistant, System.currentTimeMillis())
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(filesDir = application.filesDir)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadMessages()?.takeLast(MAX_SAVED_MESSAGES)?.let { saved ->
                _uiState.update { it.copy(messages = saved) }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputText = value, errorMessage = null) }
    }

    fun sendMessage() {
        val text = uiState.value.inputText.trim()
        if (text.isEmpty() || uiState.value.isLoading) return

        val userMessage = ChatMessage(
            id = nextMessageId(),
            content = text,
            role = ChatRole.User
        )
        val conversation = uiState.value.messages + userMessage
        val assistantId = nextMessageId() + 1

        _uiState.update {
            it.copy(
                messages = conversation + ChatMessage(
                    id = assistantId,
                    content = "",
                    role = ChatRole.Assistant
                ),
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val webSearchEnabled = getApplication<Application>()
                .getSharedPreferences("chat_bot_preferences", Application.MODE_PRIVATE)
                .getBoolean("web_search", false)

            runCatching {
                val fullContent = StringBuilder()
                repository.sendMessagesStream(conversation, webSearchEnabled) { token ->
                    fullContent.append(token)
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        val lastIndex = updated.lastIndex
                        updated[lastIndex] = updated[lastIndex].copy(content = fullContent.toString())
                        state.copy(messages = updated)
                    }
                }
                fullContent.toString()
            }.onSuccess { reply ->
                if (reply.isEmpty()) error("未收到回复")

                // 打印完整的 AI 回复
                Log.d("ChatViewModel", "========== AI 完整回复 ==========")
                Log.d("ChatViewModel", reply)
                Log.d("ChatViewModel", "========== 回复结束，总长度: ${reply.length} ==========")

                _uiState.update { it.copy(isLoading = false) }
                repository.saveMessages(uiState.value.messages.takeLast(MAX_SAVED_MESSAGES))
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "发送失败，请稍后重试"
                    )
                }
            }
        }
    }

    private fun nextMessageId(): Int {
        return (uiState.value.messages.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearMessages()
            _uiState.update {
                it.copy(
                    messages = listOf(
                        ChatMessage(1, "你好，我是智能聊天助手。", ChatRole.Assistant, System.currentTimeMillis())
                    )
                )
            }
        }
    }
}

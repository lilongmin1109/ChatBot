package com.lm.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lm.chatbot.data.ChatRepository
import com.lm.chatbot.model.ChatMessage
import com.lm.chatbot.model.ChatRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(1, "你好，我是 DeepSeek 聊天助手。", ChatRole.Assistant)
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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

        _uiState.update {
            it.copy(
                messages = conversation,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                repository.sendMessages(conversation)
            }.onSuccess { reply ->
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            id = nextMessageId(),
                            content = reply,
                            role = ChatRole.Assistant
                        ),
                        isLoading = false
                    )
                }
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
}

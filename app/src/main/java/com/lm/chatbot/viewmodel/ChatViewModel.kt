package com.lm.chatbot.viewmodel

import android.app.Application
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

private const val MAX_SAVED_MESSAGES = 100

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(1, "你好，我是智能聊天助手。", ChatRole.Assistant)
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
                val updated = uiState.value.messages + ChatMessage(
                    id = nextMessageId(),
                    content = reply,
                    role = ChatRole.Assistant
                )
                _uiState.update {
                    it.copy(messages = updated, isLoading = false)
                }
                repository.saveMessages(updated.takeLast(MAX_SAVED_MESSAGES))
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

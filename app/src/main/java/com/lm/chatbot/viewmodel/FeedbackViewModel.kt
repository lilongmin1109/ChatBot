package com.lm.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lm.chatbot.data.FeedbackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val feedbackText: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class FeedbackViewModel(
    private val repository: FeedbackRepository = FeedbackRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun onFeedbackChange(value: String) {
        _uiState.update { it.copy(feedbackText = value, errorMessage = null) }
    }

    fun submitFeedback() {
        val text = uiState.value.feedbackText.trim()
        if (text.isEmpty() || uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            repository.submitFeedback(text)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feedbackText = "",
                            successMessage = "感谢你的反馈！"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "提交失败，请稍后重试"
                        )
                    }
                }
        }
    }
}

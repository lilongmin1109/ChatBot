package com.lm.chatbot

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lm.chatbot.ui.chat.ChatScreen
import com.lm.chatbot.ui.settings.FeedbackScreen
import com.lm.chatbot.ui.settings.SettingsScreen
import com.lm.chatbot.ui.theme.ChatBotTheme
import com.lm.chatbot.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            var isDarkTheme by rememberSaveable {
                mutableStateOf(preferences.getBoolean(KEY_DARK_THEME, false))
            }
            var webSearchEnabled by rememberSaveable {
                mutableStateOf(preferences.getBoolean(KEY_WEB_SEARCH, false))
            }
            var thinkingModeEnabled by rememberSaveable {
                mutableStateOf(preferences.getBoolean(KEY_THINKING_MODE, false))
            }

            LaunchedEffect(isDarkTheme) {
                preferences.edit()
                    .putBoolean(KEY_DARK_THEME, isDarkTheme)
                    .apply()
            }

            LaunchedEffect(webSearchEnabled) {
                preferences.edit()
                    .putBoolean(KEY_WEB_SEARCH, webSearchEnabled)
                    .apply()
            }

            LaunchedEffect(thinkingModeEnabled) {
                preferences.edit()
                    .putBoolean(KEY_THINKING_MODE, thinkingModeEnabled)
                    .apply()
            }

            ChatBotTheme(darkTheme = isDarkTheme) {
                val view = LocalView.current
                val systemBarColor = MaterialTheme.colorScheme.surface.toArgb()

                SideEffect {
                    val windowInsetsController = WindowCompat.getInsetsController(window, view)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+ 使用新的 API
                        windowInsetsController.systemBarsBehavior = 
                            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        // Android 10 及以下使用传统方式
                        @Suppress("DEPRECATION")
                        window.statusBarColor = systemBarColor
                        @Suppress("DEPRECATION")
                        window.navigationBarColor = systemBarColor
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }

                    windowInsetsController.apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
                }

                var currentScreen by remember { mutableStateOf(AppScreen.Chat) }

                when (currentScreen) {
                    AppScreen.Chat -> ChatScreen(
                        onOpenSettings = { currentScreen = AppScreen.Settings }
                    )

                    AppScreen.Settings -> SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        onDarkThemeChange = { isDarkTheme = it },
                        webSearchEnabled = webSearchEnabled,
                        onWebSearchChange = { webSearchEnabled = it },
                        thinkingModeEnabled = thinkingModeEnabled,
                        onThinkingModeChange = { thinkingModeEnabled = it },
                        onBack = { currentScreen = AppScreen.Chat },
                        onOpenFeedback = { currentScreen = AppScreen.Feedback },
                        onClearChatHistory = {
                            chatViewModel.clearChatHistory()
                            currentScreen = AppScreen.Chat
                        }
                    )

                    AppScreen.Feedback -> FeedbackScreen(
                        onBack = { currentScreen = AppScreen.Settings }
                    )
                }
            }
        }
    }
}

private enum class AppScreen {
    Chat,
    Settings,
    Feedback
}

private const val PREFERENCES_NAME = "chat_bot_preferences"
private const val KEY_DARK_THEME = "dark_theme"
private const val KEY_WEB_SEARCH = "web_search"
private const val KEY_THINKING_MODE = "thinking_mode"

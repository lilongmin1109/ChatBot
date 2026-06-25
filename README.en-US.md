# ChatBot - Android AI Assistant

A modern Android AI chatbot application built with Jetpack Compose, integrated with DeepSeek API and supporting Markdown rendering.

## ✨ Features

- **Smart Conversations**: Integrated with DeepSeek large language model for intelligent responses.
- **Markdown Support**: Supports code blocks, lists, bold text, links, and other Markdown formats for better reading experience.
- **Modern UI**: Built entirely with Jetpack Compose and Material 3 design.
- **Dark Mode**: Supports system dark mode switching to protect your eyes.
- **Web Search**: Toggle web search feature in settings (depending on API support).
- **User Feedback**: Built-in feedback system connected to Supabase backend.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Networking**: Ktor Client
- **Async Processing**: Kotlin Coroutines & Flow
- **Markdown Rendering**: `compose-markdown`
- **Backend Service**: Supabase (for storing feedback)
- **Architecture Pattern**: MVVM (ViewModel, Repository, Data Sources)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/ChatBot.git
```

### 2. Configure API Keys
Add the following configuration to the `local.properties` file in the project root directory:
```properties
DEEPSEEK_API_KEY=your_DEEPSEEK_API_key
SUPABASE_URL=your_SUPABASE_project_URL
SUPABASE_KEY=your_SUPABASE_anonymous_key
```

### 3. Build and Run
Open the project in Android Studio, sync Gradle, and run it on your device or emulator.

##  App Screenshots

| Chat Screen | Settings Screen |
| :---: | :---: |
| ![Chat Screen](screenshots/chat_screen.png) | ![Settings Screen](screenshots/settings_screen.png) |

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 🌐 Languages

- [English](README.en-US.md)
- [简体中文](README.zh-CN.md)

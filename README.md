# ChatBot - Android AI Assistant

一个基于 Jetpack Compose 构建的现代 Android AI 聊天机器人应用，接入了 DeepSeek API 并支持 Markdown 渲染。

## ✨ 特性

- **智能对话**：接入 DeepSeek 大语言模型，提供智能响应。
- **Markdown 支持**：支持代码块、列表、加粗、链接等 Markdown 格式渲染，阅读体验更佳。
- **现代化 UI**：完全使用 Jetpack Compose 和 Material 3 设计。
- **深色模式**：支持系统深色模式切换，保护视力。
- **联网搜索**：可在设置中开启/关闭联网搜索功能（取决于 API 支持）。
- **用户反馈**：内置反馈系统，连接 Supabase 后端。

## 🛠️ 技术栈

- **开发语言**：Kotlin
- **UI 框架**：Jetpack Compose (Material 3)
- **网络请求**：Ktor Client
- **异步处理**：Kotlin Coroutines & Flow
- **Markdown 渲染**：`compose-markdown`
- **后端服务**：Supabase (用于存储反馈)
- **架构模式**：MVVM (ViewModel, Repository, Data Sources)

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/your-username/ChatBot.git
```

### 2. 配置 API Key
在项目根目录的 `local.properties` 文件中添加以下配置：
```properties
DEEPSEEK_API_KEY=你的_DEEPSEEK_API_密钥
SUPABASE_URL=你的_SUPABASE_项目_URL
SUPABASE_KEY=你的_SUPABASE_匿名_KEY
```

### 3. 构建运行
在 Android Studio 中打开项目，同步 Gradle 并运行到您的设备或模拟器。

## 📸 应用截图

| 聊天界面 | 设置页面 |
| :---: | :---: |
| ![Chat Screen](https://via.placeholder.com/200x400?text=Chat+Screen) | ![Settings Screen](https://via.placeholder.com/200x400?text=Settings+Screen) |

## 📄 开源协议
本项目采用 [MIT License](LICENSE) 开源。

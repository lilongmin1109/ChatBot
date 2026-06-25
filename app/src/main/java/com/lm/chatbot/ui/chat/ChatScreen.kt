package com.lm.chatbot.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.lm.chatbot.model.ChatMessage
import com.lm.chatbot.model.ChatRole
import com.lm.chatbot.ui.theme.ChatBotTheme
import com.lm.chatbot.viewmodel.ChatUiState
import com.lm.chatbot.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ChatContent(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::sendMessage,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    // 记录是否已经进行了初始滚动
    var isInitialScrollDone by remember { mutableStateOf(false) }

    // 滚动逻辑优化
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            if (!isInitialScrollDone) {
                // 1. 首次打开 App 加载历史记录后，直接定位到底部最新的消息
                listState.scrollToItem(uiState.messages.size - 1)
                isInitialScrollDone = true
            } else if (uiState.isLoading) {
                // 2. 只有在发送消息（加载中状态）时，才触发平滑滚动
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // 点击空白区域收起键盘
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        topBar = {
            ChatTopBar(onOpenSettings = onOpenSettings)
        },
        bottomBar = {
            MessageInputBar(
                value = uiState.inputText,
                isLoading = uiState.isLoading,
                onValueChange = onInputChange,
                onSend = onSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 列表顶部间距
            item { Spacer(Modifier.height(8.dp)) }

            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
                ChatBubble(message = message)
            }

            // 错误处理优化：显示为带重试按钮的卡片
            uiState.errorMessage?.let { errorMessage ->
                item(key = "error") {
                    ErrorCard(message = errorMessage, onRetry = onSend)
                }
            }

            // 列表底部间距
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ChatTopBar(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = "Chat Bot",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置"
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isAssistant = message.role == ChatRole.Assistant
    val isPlaceholder = isAssistant && message.content.isEmpty()
    val bubbleModifier = if (message.isFromUser) {
        Modifier.sizeIn(maxWidth = 300.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = bubbleModifier,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isFromUser) 18.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (message.isFromUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        ) {
            if (isPlaceholder) {
                Text(
                    text = "正在思考...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            } else {
                val textColor = if (message.isFromUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }

                MessageBody(
                    content = message.content,
                    isFromUser = message.isFromUser,
                    textColor = textColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        
        // 显示时间戳
        Text(
            text = message.getFormattedTime(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun MessageBody(
    content: String,
    isFromUser: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor)

    if (isFromUser) {
        Text(
            text = content,
            modifier = modifier,
            style = textStyle
        )
    } else {
        val markdown = remember(content) { normalizeMarkdownForDisplay(content) }

        MarkdownText(
            markdown = markdown,
            modifier = modifier,
            style = textStyle,
            linkColor = MaterialTheme.colorScheme.primary,
            syntaxHighlightColor = MaterialTheme.colorScheme.surfaceVariant,
            syntaxHighlightTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            enableSoftBreakAddsNewLine = true,
            isTextSelectable = true,
            wrapMultilineTextWidth = true
        )
    }
}

private fun normalizeMarkdownForDisplay(content: String): String {
    return content
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .splitMarkdownCodeFences()
        .joinToString("") { segment ->
            if (segment.isCodeFence) {
                segment.text
            } else {
                segment.text
                    .breakBeforeHeadings()
                    .breakHeadingBeforeTable()
                    .breakCompactBulletItems()
                    .breakCompactTableRows()
                    .trimExtraBlankLines()
            }
        }
        .trim()
}

private data class MarkdownSegment(
    val text: String,
    val isCodeFence: Boolean
)

private fun String.splitMarkdownCodeFences(): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val builder = StringBuilder()
    var isCodeFence = false

    lineSequence().forEach { line ->
        if (line.trimStart().startsWith("```")) {
            if (builder.isNotEmpty()) {
                segments += MarkdownSegment(builder.toString(), isCodeFence)
                builder.clear()
            }
            isCodeFence = !isCodeFence
            builder.appendLine(line)
        } else {
            builder.appendLine(line)
        }
    }

    if (builder.isNotEmpty()) {
        segments += MarkdownSegment(builder.toString(), isCodeFence)
    }

    return segments
}

private fun String.breakBeforeHeadings(): String {
    return replace(Regex("""([^\n])(?=#{1,6}\s)"""), "$1\n\n")
}

private fun String.breakHeadingBeforeTable(): String {
    return replace(
        Regex("""(?m)^(#{1,6}\s+[^|\n]+)(\|[^\n]+\|)"""),
        "$1\n\n$2"
    )
}

private fun String.breakCompactBulletItems(): String {
    return replace(
        Regex("""([^\n])\s*-\s+(?=(\*\*[^*\n]+?\*\*|[\p{So}\p{L}\p{N}]{1,12})[:：])"""),
        "$1\n- "
    )
}

private fun String.breakCompactTableRows(): String {
    val tableRow = """\|[^|\n]+(?:\|[^|\n]+)+\|"""
    return replace(
        Regex("""($tableRow)\s*(?=$tableRow)"""),
        "$1\n"
    )
}

private fun String.trimExtraBlankLines(): String {
    return replace(Regex("""\n{3,}"""), "\n\n")
}

@Composable
private fun MessageInputBar(
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = true, // 允许在等待时继续输入
                placeholder = { Text(text = "输入消息...") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (value.isNotBlank() && !isLoading) onSend()
                })
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (value.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("重试")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatContentPreview() {
    val currentTime = System.currentTimeMillis()
    ChatBotTheme {
        ChatContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(1, "你好，我是智能聊天助手。", ChatRole.Assistant, currentTime - 60000),
                    ChatMessage(2, "请帮我写一个周报提纲。", ChatRole.User, currentTime - 30000),
                    ChatMessage(3, "可以，我会按工作进展、问题风险、下周计划来整理。", ChatRole.Assistant, currentTime)
                )
            ),
            onInputChange = {},
            onSend = {},
            onOpenSettings = {}
        )
    }
}

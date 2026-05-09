package com.lm.chatbot.data

import com.lm.chatbot.model.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import org.json.JSONArray
import org.json.JSONObject

class DeepSeekApiClient(
    private val apiKey: String,
    baseUrl: String = "https://api.deepseek.com",
    private val httpClient: HttpClient = defaultHttpClient()
) {
    private val chatCompletionUrl = "$baseUrl/chat/completions"
    private val anthropicUrl = "$baseUrl/anthropic/v1/messages"

    suspend fun createChatCompletion(messages: List<ChatMessage>): String {
        if (apiKey.isBlank()) {
            error("请先在 local.properties 中配置 DEEPSEEK_API_KEY")
        }

        return runCatching {
            httpClient.post(chatCompletionUrl) {
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(TextContent(buildRequestBody(messages).toString(), ContentType.Application.Json))
            }.bodyAsText()
        }.fold(
            onSuccess = { responseBody ->
                parseAssistantMessage(responseBody)
            },
            onFailure = { throwable ->
                error(throwable.toDeepSeekMessage())
            }
        )
    }

    suspend fun createChatCompletionStream(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit
    ) {
        if (apiKey.isBlank()) {
            error("请先在 local.properties 中配置 DEEPSEEK_API_KEY")
        }

        val body = JSONObject()
            .put("model", "deepseek-v4-flash")
            .put("messages", buildApiMessages(messages))
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("stream", true)

        httpClient.preparePost(chatCompletionUrl) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ")
                    if (data == "[DONE]") break
                    parseStreamDelta(data)?.let { onToken(it) }
                }
            }
        }
    }

    suspend fun createWebSearchStream(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit
    ) {
        if (apiKey.isBlank()) {
            error("请先在 local.properties 中配置 DEEPSEEK_API_KEY")
        }

        val body = JSONObject()
            .put("model", "deepseek-v4-flash")
            .put("max_tokens", 4096)
            .put("messages", buildApiMessages(messages))
            .put("tools", JSONArray().put(
                JSONObject()
                    .put("type", "web_search_20250305")
                    .put("name", "web_search")
                    .put("max_uses", 3)
            ))
            .put("stream", true)

        httpClient.preparePost(anthropicUrl) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            var currentEvent: String? = null
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                when {
                    line.startsWith("event: ") -> {
                        currentEvent = line.removePrefix("event: ").trim()
                    }
                    line.startsWith("data: ") -> {
                        val data = line.removePrefix("data: ")
                        val eventType = currentEvent ?: runCatching {
                            JSONObject(data).optString("type")
                        }.getOrNull()
                        if (eventType == "content_block_delta") {
                            parseAnthropicTextDelta(data)?.let { onToken(it) }
                        }
                    }
                }
            }
        }
    }

    private fun buildRequestBody(messages: List<ChatMessage>): JSONObject {
        return JSONObject()
            .put("model", "deepseek-v4-flash")
            .put("messages", buildApiMessages(messages))
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("stream", false)
    }

    private fun buildApiMessages(messages: List<ChatMessage>): JSONArray {
        return JSONArray().apply {
            messages.forEach { message ->
                put(
                    JSONObject()
                        .put("role", message.role.apiValue)
                        .put("content", message.content)
                )
            }
        }
    }

    private fun parseStreamDelta(data: String): String? {
        return runCatching {
            JSONObject(data)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("delta")
                .optString("content")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun parseAnthropicTextDelta(data: String): String? {
        return runCatching {
            val json = JSONObject(data)
            val delta = json.getJSONObject("delta")
            if (delta.optString("type") == "text_delta") {
                delta.optString("text")
            } else null
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun parseAssistantMessage(responseBody: String): String {
        return JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .ifBlank { error("DeepSeek 未返回有效回复") }
    }

    private fun Throwable.toDeepSeekMessage(): String {
        return when (this) {
            is ClientRequestException -> response.status.description
            is ServerResponseException -> response.status.description
            else -> message ?: "DeepSeek 请求失败"
        }
    }

    companion object {
        private fun defaultHttpClient(): HttpClient {
            return HttpClient(Android) {
                engine {
                    connectTimeout = 30_000
                    socketTimeout = 60_000
                }

                defaultRequest {
                    accept(ContentType.Application.Json)
                }
            }
        }
    }
}

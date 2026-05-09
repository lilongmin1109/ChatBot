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
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject

class DeepSeekApiClient(
    private val apiKey: String,
    baseUrl: String = "https://api.deepseek.com",
    private val httpClient: HttpClient = defaultHttpClient()
) {
    private val chatCompletionUrl = "$baseUrl/chat/completions"

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

    private fun buildRequestBody(messages: List<ChatMessage>): JSONObject {
        val apiMessages = JSONArray().apply {
            messages.forEach { message ->
                put(
                    JSONObject()
                        .put("role", message.role.apiValue)
                        .put("content", message.content)
                )
            }
        }

        return JSONObject()
            .put("model", "deepseek-v4-flash")
            .put("messages", apiMessages)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("stream", false)
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

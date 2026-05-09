package com.lm.chatbot.data

import com.lm.chatbot.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import org.json.JSONObject

class FeedbackApiClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_KEY,
    private val httpClient: HttpClient = defaultHttpClient()
) {
    private val feedbackUrl = "$supabaseUrl/rest/v1/feedbacks"

    suspend fun submitFeedback(content: String) {
        val body = JSONObject().put("content", content)

        runCatching {
            httpClient.post(feedbackUrl) {
                header("apikey", supabaseKey)
                header("Authorization", "Bearer $supabaseKey")
                header("Prefer", "return=minimal")
                contentType(ContentType.Application.Json)
                setBody(TextContent(body.toString(), ContentType.Application.Json))
            }
        }.getOrElse { throwable ->
            when (throwable) {
                is ClientRequestException -> error("提交失败: ${throwable.response.status.description}")
                is ServerResponseException -> error("服务器错误: ${throwable.response.status.description}")
                else -> error(throwable.message ?: "反馈提交失败")
            }
        }
    }

    companion object {
        private fun defaultHttpClient(): HttpClient {
            return HttpClient(Android) {
                engine {
                    connectTimeout = 15_000
                    socketTimeout = 15_000
                }
            }
        }
    }
}

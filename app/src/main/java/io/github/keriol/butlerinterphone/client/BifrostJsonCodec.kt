package io.github.keriol.butlerinterphone.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal object BifrostJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun encodeRequest(
        request: InterphoneRequest,
    ): String = buildJsonObject {
        put("request_id", request.requestId)
        put("message", request.message)

        request.targetButlerName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                put("target_butler_name", it)
            }
    }.toString()

    fun decodeResponse(
        expectedRequestId: String,
        statusCode: Int,
        body: String,
    ): InterphoneResponse {
        val root = parseObject(statusCode, body)
        val requestId = root.text("request_id")

        if (requestId != null && requestId != expectedRequestId) {
            throw InterphoneClientException.CorrelationMismatch()
        }

        val ok = root["ok"]
            ?.jsonPrimitive
            ?.booleanOrNull
            ?: false

        if (statusCode !in 200..299 || !ok) {
            val error = root["error"]?.jsonObject
            val code = error?.text("code") ?: "remote_error"
            val message = error?.text("message")
                ?: "Bifröst rejected the request."

            throw InterphoneClientException.Remote(
                code = code,
                message = message,
            )
        }

        if (requestId == null) {
            throw InterphoneClientException.InvalidResponse(
                "Bifröst response has no request id."
            )
        }

        val response = root.text("response")
            ?: throw InterphoneClientException.InvalidResponse(
                "Bifröst response has no text."
            )

        return InterphoneResponse(
            requestId = requestId,
            response = response,
            sourceButlerName = root.text("source_butler_name"),
        )
    }

    private fun parseObject(
        statusCode: Int,
        body: String,
    ): JsonObject {
        if (body.isBlank()) {
            throw InterphoneClientException.InvalidResponse(
                "Bifröst returned an empty response (HTTP $statusCode)."
            )
        }

        return try {
            json.parseToJsonElement(body).jsonObject
        } catch (_: Exception) {
            val preview = body
                .replace(
                    Regex("(?i)Bearer\\s+\\S+"),
                    "Bearer [redacted]",
                )
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(240)

            throw InterphoneClientException.InvalidResponse(
                "Bifröst returned invalid JSON (HTTP $statusCode): $preview"
            )
        }
    }

    private fun JsonObject.text(
        key: String,
    ): String? = this[key]
        ?.jsonPrimitive
        ?.content
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != "null" }
}

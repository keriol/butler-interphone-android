package io.github.keriol.butlerinterphone.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
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



    fun decodeButlerDirectory(
        statusCode: Int,
        body: String,
    ): List<ButlerDirectoryEntry> {
        val root = parseObject(statusCode, body)
        val ok = root["ok"]
            ?.jsonPrimitive
            ?.booleanOrNull
            ?: false

        if (statusCode !in 200..299 || !ok) {
            val error = root["error"]?.jsonObject
            val code = error?.text("code") ?: "remote_error"
            val message = error?.text("message")
                ?: "Bifröst rejected the directory request."

            throw InterphoneClientException.Remote(
                code = code,
                message = message,
            )
        }

        val items = root["butlers"]?.jsonArray
            ?: throw InterphoneClientException.InvalidResponse(
                "Bifröst directory response has no butlers list."
            )

        return items.map { element ->
            val entry = element.jsonObject
            val canonicalName = entry.text("canonical_name")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst directory entry has no canonical name."
                )
            val aliases = entry["aliases"]
                ?.jsonArray
                ?.mapNotNull { alias ->
                    alias.jsonPrimitive.content
                        .trim()
                        .takeIf { it.isNotEmpty() }
                }
                .orEmpty()
            val available = entry["available"]
                ?.jsonPrimitive
                ?.booleanOrNull
                ?: false

            ButlerDirectoryEntry(
                canonicalName = canonicalName,
                aliases = aliases,
                available = available,
            )
        }
    }

    fun decodeNodeManifest(
        statusCode: Int,
        body: String,
    ): NodeManifest {
        val root = parseObject(statusCode, body)
        val ok = root["ok"]?.jsonPrimitive?.booleanOrNull ?: false

        if (statusCode !in 200..299 || !ok) {
            val error = root["error"]?.jsonObject
            throw InterphoneClientException.Remote(
                code = error?.text("code") ?: "remote_error",
                message = error?.text("message")
                    ?: "Bifröst rejected the manifest request.",
            )
        }

        val protocolVersion = root["protocol_version"]
            ?.jsonPrimitive
            ?.intOrNull
            ?: throw InterphoneClientException.InvalidResponse(
                "Bifröst manifest has no protocol version."
            )
        val bifrost = root["bifrost"]?.jsonObject
            ?: throw InterphoneClientException.InvalidResponse(
                "Bifröst manifest has no Bifröst metadata."
            )
        val core = root["core"]?.jsonObject
            ?: throw InterphoneClientException.InvalidResponse(
                "Bifröst manifest has no Core metadata."
            )

        return NodeManifest(
            protocolVersion = protocolVersion,
            bifrostVersion = bifrost.text("version")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest has no Bifröst version."
                ),
            core = ManifestCore(
                version = core.text("version")
                    ?: throw InterphoneClientException.InvalidResponse(
                        "Bifröst manifest has no Core version."
                    ),
                plugins = core.array("plugins").map(::decodePlugin),
            ),
            butlers = root.array("butlers").map(::decodeButler),
        )
    }

    private fun decodeButler(element: kotlinx.serialization.json.JsonElement): ManifestButler {
        val item = element.jsonObject
        return ManifestButler(
            canonicalName = item.text("canonical_name")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest Butler has no canonical name."
                ),
            aliases = item.array("aliases").mapNotNull { alias ->
                alias.jsonPrimitive.content.trim().takeIf(String::isNotEmpty)
            },
            description = item.text("description").orEmpty(),
            version = item.text("version"),
            available = item["available"]?.jsonPrimitive?.booleanOrNull ?: false,
            asgardVersion = item["asgard"]?.jsonObject?.text("version"),
            entities = item.array("entities").map(::decodeEntity),
            plugins = item.array("plugins").map(::decodePlugin),
        )
    }

    private fun decodeEntity(element: kotlinx.serialization.json.JsonElement): ManifestEntity {
        val item = element.jsonObject
        return ManifestEntity(
            name = item.text("name")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest entity has no name."
                ),
            description = item.text("description").orEmpty(),
            available = item["available"]?.jsonPrimitive?.booleanOrNull ?: false,
            readiness = item["readiness"]?.jsonObject?.let(::decodeReadiness),
            methods = item.array("methods").map(::decodeCallable),
            dependencies = item.array("dependencies").map(::decodeDependency),
        )
    }

    private fun decodeCallable(element: kotlinx.serialization.json.JsonElement): ManifestCallable {
        val item = element.jsonObject
        return ManifestCallable(
            name = item.text("name")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest callable has no name."
                ),
            description = item.text("description").orEmpty(),
            available = item["available"]?.jsonPrimitive?.booleanOrNull ?: false,
            readiness = item["readiness"]?.jsonObject?.let(::decodeReadiness),
            dependencies = item.array("dependencies").map(::decodeDependency),
        )
    }

    private fun decodePlugin(element: kotlinx.serialization.json.JsonElement): ManifestPlugin {
        val item = element.jsonObject
        return ManifestPlugin(
            name = item.text("name")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest plugin has no name."
                ),
            version = item.text("version")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest plugin has no version."
                ),
            description = item.text("description").orEmpty(),
            available = item["available"]?.jsonPrimitive?.booleanOrNull ?: false,
            readiness = item["readiness"]?.jsonObject?.let(::decodeReadiness),
            dependencies = item.array("dependencies").map(::decodeDependency),
        )
    }

    private fun decodeReadiness(item: JsonObject): ManifestReadiness = ManifestReadiness(
        state = item.text("state")
            ?: throw InterphoneClientException.InvalidResponse(
                "Bifröst manifest readiness has no state."
            ),
        reasonCode = item.text("reason_code"),
    )

    private fun decodeDependency(element: kotlinx.serialization.json.JsonElement): ManifestDependency {
        val item = element.jsonObject
        return ManifestDependency(
            name = item.text("name")
                ?: throw InterphoneClientException.InvalidResponse(
                    "Bifröst manifest dependency has no name."
                ),
            version = item.text("version"),
        )
    }

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

    private fun JsonObject.array(key: String): JsonArray =
        this[key] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.text(
        key: String,
    ): String? = this[key]
        ?.jsonPrimitive
        ?.content
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != "null" }
}

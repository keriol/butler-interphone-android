package io.github.keriol.butlerinterphone.client

import java.net.URI

data class BifrostEndpointParts(
    val protocol: String = "http",
    val host: String = "",
    val port: String = "",
) {
    fun toBaseUrl(): String {
        val normalizedProtocol = protocol.trim().ifEmpty { "http" }
        val normalizedHost = host.trim()
        val normalizedPort = port.trim()

        require(normalizedProtocol == "http" || normalizedProtocol == "https") {
            "Protocol must be http or https."
        }
        require(normalizedHost.isNotEmpty()) {
            "Bifröst host is required."
        }

        val portNumber = normalizedPort.toIntOrNull()
            ?: throw IllegalArgumentException("Bifröst port must be a number.")

        require(portNumber in 1..65535) {
            "Bifröst port must be between 1 and 65535."
        }

        return "$normalizedProtocol://$normalizedHost:$portNumber"
    }

    companion object {
        fun fromUrl(value: String): BifrostEndpointParts {
            val raw = value.trim()
            if (raw.isEmpty()) {
                return BifrostEndpointParts()
            }

            return try {
                val uri = URI(raw)
                BifrostEndpointParts(
                    protocol = uri.scheme
                        ?.takeIf { it == "http" || it == "https" }
                        ?: "http",
                    host = uri.host.orEmpty(),
                    port = uri.port
                        .takeIf { it > 0 }
                        ?.toString()
                        .orEmpty(),
                )
            } catch (_: Exception) {
                BifrostEndpointParts()
            }
        }
    }
}

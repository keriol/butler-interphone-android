package io.github.keriol.butlerinterphone.client

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BifrostHttpClient(
    private val baseUrl: String,
    private val token: String,
    private val timeoutMillis: Int = 15_000,
    private val executor: HttpExecutor = UrlConnectionHttpExecutor(),
) : InterphoneClient {

    override suspend fun send(
        request: InterphoneRequest,
    ): InterphoneResponse {
        val endpoint = baseUrl.trim().trimEnd('/')
        if (endpoint.isEmpty()) {
            throw InterphoneClientException.Configuration(
                "Bifröst endpoint is not configured."
            )
        }
        if (token.isBlank()) {
            throw InterphoneClientException.Configuration(
                "Bifröst token is not configured."
            )
        }

        val response = executor.post(
            url = "$endpoint/bifrost/v1/text",
            bearerToken = token,
            body = BifrostJsonCodec.encodeRequest(request),
            timeoutMillis = timeoutMillis,
        )

        return BifrostJsonCodec.decodeResponse(
            expectedRequestId = request.requestId,
            statusCode = response.statusCode,
            body = response.body,
        )
    }
}

sealed class InterphoneClientException(
    message: String,
) : Exception(message) {

    class Configuration(message: String) :
        InterphoneClientException(message)

    class Transport(message: String) :
        InterphoneClientException(message)

    class Remote(
        val code: String,
        message: String,
    ) : InterphoneClientException("$code: $message")

    class InvalidResponse(message: String) :
        InterphoneClientException(message)

    class CorrelationMismatch :
        InterphoneClientException("Correlation mismatch.")
}

internal data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

internal fun interface HttpExecutor {
    suspend fun post(
        url: String,
        bearerToken: String,
        body: String,
        timeoutMillis: Int,
    ): HttpResponse
}

internal class UrlConnectionHttpExecutor : HttpExecutor {
    override suspend fun post(
        url: String,
        bearerToken: String,
        body: String,
        timeoutMillis: Int,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (_: Exception) {
            throw InterphoneClientException.Transport(
                "Could not open the Bifröst connection."
            )
        }

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.doOutput = true
            connection.setRequestProperty(
                "Authorization",
                "Bearer $bearerToken",
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8",
            )
            connection.setRequestProperty(
                "Accept",
                "application/json",
            )

            connection.outputStream.bufferedWriter(
                Charsets.UTF_8
            ).use { writer ->
                writer.write(body)
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            HttpResponse(
                statusCode = statusCode,
                body = responseBody,
            )
        } catch (_: IOException) {
            throw InterphoneClientException.Transport(
                "Bifröst request failed."
            )
        } finally {
            connection.disconnect()
        }
    }
}

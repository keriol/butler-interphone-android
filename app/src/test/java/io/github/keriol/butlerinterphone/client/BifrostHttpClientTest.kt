package io.github.keriol.butlerinterphone.client

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BifrostHttpClientTest {

    @Test
    fun targetlessRequestOmitsButlerNameAndPreservesCorrelation() = runTest {
        var capturedBody: String? = null

        val client = BifrostHttpClient(
            baseUrl = "https://example.test",
            token = "test-token",
            executor = HttpExecutor { _, _, body, _ ->
                capturedBody = body
                HttpResponse(
                    statusCode = 200,
                    body = """
                        {
                          "ok": true,
                          "request_id": "req-1",
                          "source_butler_name": null,
                          "response": "Done."
                        }
                    """.trimIndent(),
                )
            },
        )

        val result = client.send(
            InterphoneRequest(
                requestId = "req-1",
                message = "turn off the light",
            )
        )

        val json = Json.parseToJsonElement(
            checkNotNull(capturedBody)
        ).jsonObject

        assertEquals("req-1", json["request_id"]?.jsonPrimitive?.content)
        assertEquals(
            "turn off the light",
            json["message"]?.jsonPrimitive?.content,
        )
        assertFalse("target_butler_name" in json)
        assertEquals("req-1", result.requestId)
        assertEquals("Done.", result.response)
        assertNull(result.sourceButlerName)
    }

    @Test
    fun explicitButlerTargetIsSerializedWhenRequested() = runTest {
        var capturedBody: String? = null

        val client = BifrostHttpClient(
            baseUrl = "https://example.test",
            token = "test-token",
            executor = HttpExecutor { _, _, body, _ ->
                capturedBody = body
                HttpResponse(
                    statusCode = 200,
                    body = """
                        {
                          "ok": true,
                          "request_id": "req-2",
                          "source_butler_name": "Butler-A",
                          "response": "Ready."
                        }
                    """.trimIndent(),
                )
            },
        )

        client.send(
            InterphoneRequest(
                requestId = "req-2",
                message = "hello",
                targetButlerName = "Butler-A",
            )
        )

        val json = Json.parseToJsonElement(
            checkNotNull(capturedBody)
        ).jsonObject

        assertEquals(
            "Butler-A",
            json["target_butler_name"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun structuredRemoteErrorBecomesTypedFailure() {
        val client = BifrostHttpClient(
            baseUrl = "https://example.test",
            token = "test-token",
            executor = HttpExecutor { _, _, _, _ ->
                HttpResponse(
                    statusCode = 503,
                    body = """
                        {
                          "ok": false,
                          "request_id": "req-3",
                          "error": {
                            "code": "midgard_unavailable",
                            "message": "Routing layer unavailable."
                          }
                        }
                    """.trimIndent(),
                )
            },
        )

        val failure = assertThrows(
            InterphoneClientException.Remote::class.java
        ) {
            runBlocking {
                client.send(
                    InterphoneRequest(
                        requestId = "req-3",
                        message = "hello",
                    )
                )
            }
        }

        assertEquals("midgard_unavailable", failure.code)
    }

    @Test
    fun correlationMismatchIsRejected() {
        val failure = assertThrows(
            InterphoneClientException.CorrelationMismatch::class.java
        ) {
            BifrostJsonCodec.decodeResponse(
                expectedRequestId = "expected",
                statusCode = 200,
                body = """
                    {
                      "ok": true,
                      "request_id": "wrong",
                      "response": "Nope."
                    }
                """.trimIndent(),
            )
        }

        assertEquals("Correlation mismatch.", failure.message)
    }
}

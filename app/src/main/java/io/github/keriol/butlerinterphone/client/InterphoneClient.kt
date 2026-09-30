package io.github.keriol.butlerinterphone.client

data class InterphoneRequest(
    val requestId: String,
    val message: String,
)

data class InterphoneResponse(
    val requestId: String,
    val response: String,
)

interface InterphoneClient {
    suspend fun send(
        request: InterphoneRequest,
    ): InterphoneResponse
}

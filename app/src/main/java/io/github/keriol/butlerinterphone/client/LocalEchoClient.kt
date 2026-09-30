package io.github.keriol.butlerinterphone.client

import kotlinx.coroutines.delay

class LocalEchoClient(
    private val delayMillis: Long = 150,
) : InterphoneClient {
    override suspend fun send(
        request: InterphoneRequest,
    ): InterphoneResponse {
        delay(delayMillis)

        return InterphoneResponse(
            requestId = request.requestId,
            response = "Echo: ${request.message}",
        )
    }
}

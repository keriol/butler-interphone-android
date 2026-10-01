package io.github.keriol.butlerinterphone.client

data class InterphoneRequest(
    val requestId: String,
    val message: String,
    val targetButlerName: String? = null,
)

data class InterphoneResponse(
    val requestId: String,
    val response: String,
    val sourceButlerName: String? = null,
)

data class ButlerDirectoryEntry(
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val available: Boolean = true,
)

interface InterphoneClient {
    suspend fun send(
        request: InterphoneRequest,
    ): InterphoneResponse

    suspend fun listButlers(): List<ButlerDirectoryEntry> = emptyList()
}

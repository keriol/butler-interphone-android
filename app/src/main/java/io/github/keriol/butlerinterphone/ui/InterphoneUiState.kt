package io.github.keriol.butlerinterphone.ui

enum class RequestPhase {
    Idle,
    Sending,
    Success,
    Error,
}

data class InterphoneUiState(
    val protocol: String = "http",
    val host: String = "",
    val port: String = "",
    val token: String = "",
    val targetButlerName: String = "",
    val message: String = "",
    val phase: RequestPhase = RequestPhase.Idle,
    val response: String? = null,
    val requestId: String? = null,
    val sourceButlerName: String? = null,
    val error: String? = null,
)

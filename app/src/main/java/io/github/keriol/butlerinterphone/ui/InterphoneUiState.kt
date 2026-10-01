package io.github.keriol.butlerinterphone.ui

enum class RequestPhase {
    Idle,
    Sending,
    Success,
    Error,
}

data class InterphoneUiState(
    val endpoint: String = "",
    val token: String = "",
    val message: String = "",
    val phase: RequestPhase = RequestPhase.Idle,
    val response: String? = null,
    val requestId: String? = null,
    val error: String? = null,
)

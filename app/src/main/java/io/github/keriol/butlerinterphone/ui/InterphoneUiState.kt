package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.ButlerDirectoryEntry

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
    val availableButlers: List<ButlerDirectoryEntry> = emptyList(),
    val directoryLoading: Boolean = false,
    val directoryError: String? = null,
    val message: String = "",
    val phase: RequestPhase = RequestPhase.Idle,
    val response: String? = null,
    val requestId: String? = null,
    val sourceButlerName: String? = null,
    val error: String? = null,
)

package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.ButlerDirectoryEntry
import io.github.keriol.butlerinterphone.client.NodeManifest

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
    val nodeManifest: NodeManifest? = null,
    val manifestLoading: Boolean = false,
    val manifestError: String? = null,
    val compatibilityError: String? = null,
    val compatibilityWarnings: List<String> = emptyList(),
    val incompatibleButlers: Map<String, String> = emptyMap(),
    val configSaved: Boolean = false,
    val message: String = "",
    val phase: RequestPhase = RequestPhase.Idle,
    val response: String? = null,
    val requestId: String? = null,
    val sourceButlerName: String? = null,
    val error: String? = null,
)

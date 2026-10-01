package io.github.keriol.butlerinterphone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.client.InterphoneRequest
import io.github.keriol.butlerinterphone.settings.BifrostConnectionSettings
import io.github.keriol.butlerinterphone.settings.ConnectionSettingsStore
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InterphoneViewModel(
    private val clientFactory: (String, String) -> InterphoneClient,
    private val settingsStore: ConnectionSettingsStore,
    initialEndpoint: BifrostEndpointParts = BifrostEndpointParts(),
    initialToken: String = "",
    private val requestIdFactory: () -> String = {
        UUID.randomUUID().toString()
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        settingsStore.load()?.let {
            InterphoneUiState(
                protocol = it.protocol,
                host = it.host,
                port = it.port,
                token = it.token,
            )
        } ?: InterphoneUiState(
            protocol = initialEndpoint.protocol,
            host = initialEndpoint.host,
            port = initialEndpoint.port,
            token = initialToken,
        )
    )

    val uiState: StateFlow<InterphoneUiState> =
        _uiState.asStateFlow()

    init {
        if (hasUsableConnection(_uiState.value)) {
            refreshButlers()
        }
    }

    fun onProtocolChanged(protocol: String) {
        _uiState.update {
            it.copy(
                protocol = protocol,
                error = null,
            )
        }
    }

    fun onHostChanged(host: String) {
        _uiState.update {
            it.copy(
                host = host,
                error = null,
            )
        }
    }

    fun onPortChanged(port: String) {
        _uiState.update {
            it.copy(
                port = port,
                error = null,
            )
        }
    }

    fun onTokenChanged(token: String) {
        _uiState.update {
            it.copy(
                token = token,
                error = null,
            )
        }
    }

    fun onTargetButlerChanged(targetButlerName: String) {
        _uiState.update {
            it.copy(
                targetButlerName = targetButlerName,
                error = null,
            )
        }
    }

    fun refreshButlers() {
        val state = _uiState.value
        val endpoint = try {
            BifrostEndpointParts(
                protocol = state.protocol,
                host = state.host,
                port = state.port,
            ).toBaseUrl()
        } catch (exc: IllegalArgumentException) {
            _uiState.update {
                it.copy(
                    directoryLoading = false,
                    directoryError = exc.message ?: "Invalid Bifröst endpoint.",
                )
            }
            return
        }

        val token = state.token.trim()
        if (token.isEmpty()) {
            _uiState.update {
                it.copy(
                    directoryLoading = false,
                    directoryError = "Bifröst token is required.",
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                directoryLoading = true,
                directoryError = null,
            )
        }

        viewModelScope.launch {
            try {
                val discovered = clientFactory(endpoint, token)
                    .listButlers()
                    .filter { entry -> entry.available }

                _uiState.update {
                    it.copy(
                        availableButlers = discovered,
                        directoryLoading = false,
                        directoryError = null,
                    )
                }
            } catch (exc: Exception) {
                _uiState.update {
                    it.copy(
                        directoryLoading = false,
                        directoryError = (
                            exc.message
                                ?.takeIf { message -> message.isNotBlank() }
                                ?: "Could not load Butler directory."
                        ),
                    )
                }
            }
        }
    }

    fun onMessageChanged(message: String) {
        _uiState.update {
            it.copy(
                message = message,
                error = null,
            )
        }
    }

    fun send() {
        val state = _uiState.value
        val token = state.token.trim()
        val message = state.message.trim()
        val targetButlerName = state.targetButlerName.trim().ifEmpty { null }
        val endpoint = try {
            BifrostEndpointParts(
                protocol = state.protocol,
                host = state.host,
                port = state.port,
            ).toBaseUrl()
        } catch (exc: IllegalArgumentException) {
            _uiState.update {
                it.copy(
                    phase = RequestPhase.Error,
                    error = exc.message ?: "Invalid Bifröst endpoint.",
                    response = null,
                )
            }
            return
        }

        val validationError = when {
            token.isEmpty() -> "Bifröst token is required."
            message.isEmpty() -> "Message cannot be empty."
            else -> null
        }

        if (validationError != null) {
            _uiState.update {
                it.copy(
                    phase = RequestPhase.Error,
                    error = validationError,
                    response = null,
                )
            }
            return
        }

        settingsStore.save(
            BifrostConnectionSettings(
                protocol = state.protocol.trim().ifEmpty { "http" },
                host = state.host.trim(),
                port = state.port.trim(),
                token = token,
            )
        )

        val requestId = requestIdFactory()

        _uiState.update {
            it.copy(
                phase = RequestPhase.Sending,
                requestId = requestId,
                response = null,
                sourceButlerName = null,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val client = clientFactory(endpoint, token)
                val response = client.send(
                    InterphoneRequest(
                        requestId = requestId,
                        message = message,
                        targetButlerName = targetButlerName,
                    )
                )

                if (response.requestId != requestId) {
                    _uiState.update {
                        it.copy(
                            phase = RequestPhase.Error,
                            error = "Correlation mismatch.",
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        phase = RequestPhase.Success,
                        response = response.response,
                        sourceButlerName = response.sourceButlerName,
                        error = null,
                    )
                }
            } catch (exc: Exception) {
                _uiState.update {
                    it.copy(
                        phase = RequestPhase.Error,
                        error = (
                            exc.message
                                ?.takeIf { message -> message.isNotBlank() }
                                ?: "Request failed."
                        ),
                        response = null,
                    )
                }
            }
        }
    }
}

private fun hasUsableConnection(
    state: InterphoneUiState,
): Boolean = (
    state.protocol.isNotBlank()
        && state.host.isNotBlank()
        && state.port.isNotBlank()
        && state.token.isNotBlank()
)

class InterphoneViewModelFactory(
    private val clientFactory: (String, String) -> InterphoneClient,
    private val settingsStore: ConnectionSettingsStore,
    private val initialEndpoint: BifrostEndpointParts,
    private val initialToken: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
    ): T {
        require(
            modelClass.isAssignableFrom(
                InterphoneViewModel::class.java
            )
        )
        return InterphoneViewModel(
            clientFactory = clientFactory,
            settingsStore = settingsStore,
            initialEndpoint = initialEndpoint,
            initialToken = initialToken,
        ) as T
    }
}

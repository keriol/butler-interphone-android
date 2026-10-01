package io.github.keriol.butlerinterphone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.client.InterphoneRequest
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InterphoneViewModel(
    private val clientFactory: (String, String) -> InterphoneClient,
    initialEndpoint: String = "",
    initialToken: String = "",
    private val requestIdFactory: () -> String = {
        UUID.randomUUID().toString()
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InterphoneUiState(
            endpoint = initialEndpoint,
            token = initialToken,
        )
    )

    val uiState: StateFlow<InterphoneUiState> =
        _uiState.asStateFlow()

    fun onEndpointChanged(endpoint: String) {
        _uiState.update {
            it.copy(
                endpoint = endpoint,
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
        val endpoint = state.endpoint.trim()
        val token = state.token.trim()
        val message = state.message.trim()

        val validationError = when {
            endpoint.isEmpty() -> "Bifröst endpoint is required."
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

        val requestId = requestIdFactory()

        _uiState.update {
            it.copy(
                phase = RequestPhase.Sending,
                requestId = requestId,
                response = null,
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

class InterphoneViewModelFactory(
    private val clientFactory: (String, String) -> InterphoneClient,
    private val initialEndpoint: String,
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
            initialEndpoint = initialEndpoint,
            initialToken = initialToken,
        ) as T
    }
}

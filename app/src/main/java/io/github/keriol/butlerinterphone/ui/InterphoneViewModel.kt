package io.github.keriol.butlerinterphone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.client.InterphoneRequest
import io.github.keriol.butlerinterphone.client.LocalEchoClient
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InterphoneViewModel(
    private val client: InterphoneClient = LocalEchoClient(),
    private val requestIdFactory: () -> String = {
        UUID.randomUUID().toString()
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InterphoneUiState()
    )

    val uiState: StateFlow<InterphoneUiState> =
        _uiState.asStateFlow()

    fun onMessageChanged(message: String) {
        _uiState.update {
            it.copy(
                message = message,
                error = null,
            )
        }
    }

    fun send() {
        val message = _uiState.value.message.trim()

        if (message.isEmpty()) {
            _uiState.update {
                it.copy(
                    phase = RequestPhase.Error,
                    error = "Message cannot be empty.",
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
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        phase = RequestPhase.Error,
                        error = "Request failed.",
                        response = null,
                    )
                }
            }
        }
    }
}

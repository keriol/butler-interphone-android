package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.settings.ConnectionSettingsStore

@Composable
fun InterphoneRoute(
    initialEndpoint: BifrostEndpointParts,
    initialToken: String,
    settingsStore: ConnectionSettingsStore,
    clientFactory: (String, String) -> InterphoneClient,
) {
    val viewModel: InterphoneViewModel = viewModel(
        factory = InterphoneViewModelFactory(
            clientFactory = clientFactory,
            settingsStore = settingsStore,
            initialEndpoint = initialEndpoint,
            initialToken = initialToken,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InterphoneScreen(
        state = state,
        onProtocolChanged = viewModel::onProtocolChanged,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onTokenChanged = viewModel::onTokenChanged,
        onMessageChanged = viewModel::onMessageChanged,
        onSend = viewModel::send,
    )
}

@Composable
fun InterphoneScreen(
    state: InterphoneUiState,
    onProtocolChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Butler Interphone",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = state.protocol,
                onValueChange = onProtocolChanged,
                modifier = Modifier.width(110.dp),
                enabled = state.phase != RequestPhase.Sending,
                singleLine = true,
                label = {
                    Text("Protocol")
                },
                placeholder = {
                    Text("http")
                },
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChanged,
                modifier = Modifier.weight(1f),
                enabled = state.phase != RequestPhase.Sending,
                singleLine = true,
                label = {
                    Text("Host")
                },
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = state.port,
                onValueChange = onPortChanged,
                modifier = Modifier.width(100.dp),
                enabled = state.phase != RequestPhase.Sending,
                singleLine = true,
                label = {
                    Text("Port")
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = {
                Text("Bearer token")
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            label = {
                Text("Message")
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSend,
            enabled = (
                state.phase != RequestPhase.Sending
                    && state.host.isNotBlank()
                    && state.port.isNotBlank()
                    && state.token.isNotBlank()
                    && state.message.isNotBlank()
            ),
        ) {
            Text(
                if (state.phase == RequestPhase.Sending) {
                    "Sending…"
                } else {
                    "Send"
                }
            )
        }

        if (state.phase == RequestPhase.Sending) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.requestId?.let { requestId ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Request ID: $requestId",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        val output = state.response ?: state.error

        output?.let { value ->
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = {
                    Text("Output")
                },
                minLines = 3,
                supportingText = {
                    Text("Long-press to select and copy")
                },
            )
        }
    }
}

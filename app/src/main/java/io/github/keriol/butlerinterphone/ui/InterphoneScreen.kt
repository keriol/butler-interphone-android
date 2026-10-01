package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import io.github.keriol.butlerinterphone.client.InterphoneClient

@Composable
fun InterphoneRoute(
    initialEndpoint: String,
    initialToken: String,
    clientFactory: (String, String) -> InterphoneClient,
) {
    val viewModel: InterphoneViewModel = viewModel(
        factory = InterphoneViewModelFactory(
            clientFactory = clientFactory,
            initialEndpoint = initialEndpoint,
            initialToken = initialToken,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InterphoneScreen(
        state = state,
        onEndpointChanged = viewModel::onEndpointChanged,
        onTokenChanged = viewModel::onTokenChanged,
        onMessageChanged = viewModel::onMessageChanged,
        onSend = viewModel::send,
    )
}

@Composable
fun InterphoneScreen(
    state: InterphoneUiState,
    onEndpointChanged: (String) -> Unit,
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

        OutlinedTextField(
            value = state.endpoint,
            onValueChange = onEndpointChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            singleLine = true,
            label = {
                Text("Bifröst endpoint")
            },
            placeholder = {
                Text("http://host:port")
            },
        )

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
                    && state.endpoint.isNotBlank()
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

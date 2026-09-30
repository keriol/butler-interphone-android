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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun InterphoneRoute(
    viewModel: InterphoneViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InterphoneScreen(
        state = state,
        onMessageChanged = viewModel::onMessageChanged,
        onSend = viewModel::send,
    )
}

@Composable
fun InterphoneScreen(
    state: InterphoneUiState,
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

        state.response?.let { response ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = response,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

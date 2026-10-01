package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.settings.ConnectionSettingsStore

@Composable
fun InterphoneRoute(
    buildIdentity: String,
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
        buildIdentity = buildIdentity,
        state = state,
        onProtocolChanged = viewModel::onProtocolChanged,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onTokenChanged = viewModel::onTokenChanged,
        onTargetButlerChanged = viewModel::onTargetButlerChanged,
        onRefreshButlers = viewModel::refreshButlers,
        onMessageChanged = viewModel::onMessageChanged,
        onSend = viewModel::send,
    )
}

internal fun isConnectionConfigured(
    state: InterphoneUiState,
): Boolean = (
    state.protocol.isNotBlank()
        && state.host.isNotBlank()
        && state.port.isNotBlank()
        && state.token.isNotBlank()
)

@Composable
fun InterphoneScreen(
    buildIdentity: String,
    state: InterphoneUiState,
    onProtocolChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onTargetButlerChanged: (String) -> Unit,
    onRefreshButlers: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    val connectionConfigured = isConnectionConfigured(state)
    var showConnectionSettings by remember {
        mutableStateOf(!connectionConfigured)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Butler Interphone",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = buildIdentity,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Talking to",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = state.targetButlerName.trim().ifEmpty {
                "Butler Core"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (state.targetButlerName.isBlank()) {
                "No Butler target selected. Requests use the Core-facing path."
            } else {
                "Requests are routed to this Butler through Bifröst and Midgard."
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        state.sourceButlerName?.let { sourceButlerName ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last response from $sourceButlerName",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        var targetMenuExpanded by remember {
            mutableStateOf(false)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box {
                OutlinedButton(
                    onClick = {
                        targetMenuExpanded = true
                    },
                    enabled = state.phase != RequestPhase.Sending,
                ) {
                    Text(
                        state.targetButlerName.trim().ifEmpty {
                            "Butler Core"
                        }
                    )
                }

                DropdownMenu(
                    expanded = targetMenuExpanded,
                    onDismissRequest = {
                        targetMenuExpanded = false
                    },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Butler Core")
                        },
                        onClick = {
                            onTargetButlerChanged("")
                            targetMenuExpanded = false
                        },
                    )

                    state.availableButlers.forEach { butler ->
                        DropdownMenuItem(
                            text = {
                                Text(butler.canonicalName)
                            },
                            onClick = {
                                onTargetButlerChanged(
                                    butler.canonicalName
                                )
                                targetMenuExpanded = false
                            },
                        )
                    }
                }
            }

            TextButton(
                onClick = onRefreshButlers,
                enabled = (
                    state.phase != RequestPhase.Sending
                        && !state.directoryLoading
                        && connectionConfigured
                ),
            ) {
                Text(
                    if (state.directoryLoading) {
                        "Loading…"
                    } else {
                        "Refresh"
                    }
                )
            }
        }

        state.directoryError?.let { directoryError ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = directoryError,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What this Interphone can do",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "• Send requests to Butler Core when no Butler is selected.\n" +
                "• Route requests to a specific Butler when you select one.\n" +
                "• Preserve request correlation and show the responding Butler.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Ask",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            label = {
                Text("Message")
            },
            minLines = 2,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (connectionConfigured) {
                    showConnectionSettings = false
                }
                onSend()
            },
            enabled = (
                state.phase != RequestPhase.Sending
                    && connectionConfigured
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

        if (!connectionConfigured) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connection setup is required before sending.",
                style = MaterialTheme.typography.bodySmall,
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

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Connection",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (connectionConfigured) {
                        "Configured"
                    } else {
                        "Setup required"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (connectionConfigured && !showConnectionSettings) {
                TextButton(
                    onClick = {
                        showConnectionSettings = true
                    },
                ) {
                    Text("Edit")
                }
            } else if (connectionConfigured) {
                OutlinedButton(
                    onClick = {
                        showConnectionSettings = false
                    },
                ) {
                    Text("Hide")
                }
            }
        }

        if (showConnectionSettings || !connectionConfigured) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.protocol,
                onValueChange = onProtocolChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.phase != RequestPhase.Sending,
                singleLine = true,
                label = {
                    Text("Protocol")
                },
                placeholder = {
                    Text("http")
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.phase != RequestPhase.Sending,
                singleLine = true,
                label = {
                    Text("Host")
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.port,
                onValueChange = onPortChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.phase != RequestPhase.Sending,
                singleLine = true,
                label = {
                    Text("Port")
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

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
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.keriol.butlerinterphone.R
import io.github.keriol.butlerinterphone.branding.DefaultInterphoneBranding
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.settings.ConnectionSettingsStore

private enum class InterphoneArea(
    val title: String,
    val primary: Boolean = true,
) {
    Text("Text to your Butler"),
    Voice("Talk to your Butler"),
    Butler("Butler on Bifröst"),
    About("About"),
    Config("Connection", primary = false),
}

@Composable
fun InterphoneRoute(
    appVersion: String,
    buildDate: String,
    buildType: String,
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
        appVersion = appVersion,
        buildDate = buildDate,
        buildType = buildType,
        state = state,
        onProtocolChanged = viewModel::onProtocolChanged,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onTokenChanged = viewModel::onTokenChanged,
        onTargetButlerChanged = viewModel::onTargetButlerChanged,
        onRefreshButlers = viewModel::refreshButlers,
        onRefreshManifest = viewModel::refreshManifest,
        onSaveConfig = viewModel::saveConfig,
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
    appVersion: String,
    buildDate: String,
    buildType: String,
    state: InterphoneUiState,
    onProtocolChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onTargetButlerChanged: (String) -> Unit,
    onRefreshButlers: () -> Unit,
    onRefreshManifest: () -> Unit,
    onSaveConfig: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    var areaName by rememberSaveable {
        mutableStateOf(InterphoneArea.Text.name)
    }
    val area = InterphoneArea.valueOf(areaName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = DefaultInterphoneBranding.appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = DefaultInterphoneBranding.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = { areaName = InterphoneArea.Config.name },
            ) {
                Text("Connection")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InterphoneArea.entries.filter { it.primary }.forEach { candidate ->
                if (candidate == area) {
                    Button(onClick = {}) {
                        Text(candidate.title)
                    }
                } else {
                    OutlinedButton(
                        onClick = { areaName = candidate.name },
                    ) {
                        Text(candidate.title)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        when (area) {
            InterphoneArea.Text -> TalkArea(
                state = state,
                onMessageChanged = onMessageChanged,
                onSend = onSend,
            )
            InterphoneArea.Voice -> VoiceArea()
            InterphoneArea.Butler -> ButlerArea(
                state = state,
                onRefresh = onRefreshManifest,
            )
            InterphoneArea.About -> AboutArea(
                appVersion = appVersion,
                buildDate = buildDate,
                buildType = buildType,
                onOpenConnection = { areaName = InterphoneArea.Config.name },
            )
            InterphoneArea.Config -> ConfigArea(
                state = state,
                onProtocolChanged = onProtocolChanged,
                onHostChanged = onHostChanged,
                onPortChanged = onPortChanged,
                onTokenChanged = onTokenChanged,
                onTargetButlerChanged = onTargetButlerChanged,
                onRefreshButlers = onRefreshButlers,
                onSave = onSaveConfig,
            )
        }
    }
}

internal fun talkTargetLabel(
    targetButlerName: String,
): String = targetButlerName.trim().ifEmpty { "Butler Core" }

internal fun talkRouteLabel(
    targetButlerName: String,
): String = if (targetButlerName.isBlank()) {
    "Core-facing path"
} else {
    "Via Bifröst and Midgard"
}

@Composable
private fun TalkArea(
    state: InterphoneUiState,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    val connectionConfigured = isConnectionConfigured(state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = "${talkTargetLabel(state.targetButlerName)} • ${talkRouteLabel(state.targetButlerName)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        state.sourceButlerName?.let { source ->
            Text(
                text = "Last response from $source",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            label = { Text("Message") },
            minLines = 2,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onSend,
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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Open Connection to complete the Bifröst setup.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.phase == RequestPhase.Sending) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        val output = state.response ?: state.error
        output?.let { value ->
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text("Output") },
                minLines = 1,
                maxLines = 8,
                supportingText = { Text("Long-press to select and copy") },
            )
        }

        state.requestId?.let { requestId ->
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticCard(
                requestId = requestId,
                sourceButlerName = state.sourceButlerName,
            )
        }
    }
}

@Composable
private fun DiagnosticCard(
    requestId: String,
    sourceButlerName: String?,
) {
    var expanded by rememberSaveable(requestId) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Request diagnostics",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (expanded) "▾" else "▸",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Request ID: $requestId",
                    style = MaterialTheme.typography.bodySmall,
                )
                sourceButlerName?.let {
                    Text(
                        text = "Source Butler: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigArea(
    state: InterphoneUiState,
    onProtocolChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onTargetButlerChanged: (String) -> Unit,
    onRefreshButlers: () -> Unit,
    onSave: () -> Unit,
) {
    var targetMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "Default Butler",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box {
                OutlinedButton(
                    onClick = { targetMenuExpanded = true },
                    enabled = state.phase != RequestPhase.Sending,
                ) {
                    Text(
                        state.targetButlerName.trim().ifEmpty { "Butler Core" }
                    )
                }

                DropdownMenu(
                    expanded = targetMenuExpanded,
                    onDismissRequest = { targetMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Butler Core") },
                        onClick = {
                            onTargetButlerChanged("")
                            targetMenuExpanded = false
                        },
                    )

                    state.availableButlers.forEach { butler ->
                        DropdownMenuItem(
                            text = { Text(butler.canonicalName) },
                            onClick = {
                                onTargetButlerChanged(butler.canonicalName)
                                targetMenuExpanded = false
                            },
                        )
                    }
                }
            }

            TextButton(
                onClick = onRefreshButlers,
                enabled = (
                    !state.directoryLoading
                        && isConnectionConfigured(state)
                ),
            ) {
                Text(
                    if (state.directoryLoading) "Loading…" else "Refresh"
                )
            }
        }

        state.directoryError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = state.protocol,
            onValueChange = onProtocolChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Protocol") },
            placeholder = { Text("http") },
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.host,
            onValueChange = onHostChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Host") },
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.port,
            onValueChange = onPortChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Port") },
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Bearer token") },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSave) {
            Text("Save Config")
        }

        if (state.configSaved) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saved. Directory and manifest refreshed.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ButlerArea(
    state: InterphoneUiState,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.manifestLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        state.manifestError?.let {
            Text(
                text = it,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val manifest = state.nodeManifest
        if (manifest == null && !state.manifestLoading) {
            Text(
                text = "No node manifest loaded.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        manifest ?: return@Column

        ButlerRuntimeExplorer(
            manifest = manifest,
            onRefresh = onRefresh,
            refreshEnabled = (
                !state.manifestLoading
                    && isConnectionConfigured(state)
            ),
        )
    }
}

@Composable
private fun VoiceArea() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            text = "Talk to your Butler",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Voice conversation is the next Interphone step.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Coming after the 0.0.1 release.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutArea(
    appVersion: String,
    buildDate: String,
    buildType: String,
    onOpenConnection: () -> Unit,
) {
    val branding = DefaultInterphoneBranding
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = branding.appName + " logo",
            modifier = Modifier.size(88.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = branding.appName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = branding.tagline,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = branding.description,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
            ) {
                Text(
                    text = "Release",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Version $appVersion")
                Text("Build date $buildDate")
                Text("Build type $buildType")
                Text(branding.license)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Project",
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(
            onClick = { uriHandler.openUri(branding.projectUrl) },
        ) {
            Text(branding.projectName)
        }

        Text(
            text = "Repositories",
            style = MaterialTheme.typography.titleMedium,
        )
        branding.repositories.forEach { repository ->
            TextButton(
                onClick = { uriHandler.openUri(repository.url) },
            ) {
                Text(repository.label)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Support the project",
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(
            onClick = { uriHandler.openUri(branding.supportUrl) },
        ) {
            Text(branding.supportLabel)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenConnection,
        ) {
            Text("Connection settings")
        }
    }
}

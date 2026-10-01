package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.ManifestDependency
import io.github.keriol.butlerinterphone.client.ManifestEntity
import io.github.keriol.butlerinterphone.client.ManifestPlugin
import io.github.keriol.butlerinterphone.client.ManifestReadiness
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.settings.ConnectionSettingsStore

private enum class InterphoneArea(
    val title: String,
) {
    Talk("Talk"),
    Butler("Butler on Bifröst"),
    Config("Config"),
    Version("Version"),
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
        mutableStateOf(InterphoneArea.Talk.name)
    }
    val area = InterphoneArea.valueOf(areaName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Butler Interphone",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = when (area) {
                    InterphoneArea.Talk -> "Talk"
                    InterphoneArea.Butler -> "Runtime self-description"
                    InterphoneArea.Config -> "Client configuration"
                    InterphoneArea.Version -> "App build identity"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InterphoneArea.entries.forEach { candidate ->
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
            InterphoneArea.Talk -> TalkArea(
                state = state,
                onMessageChanged = onMessageChanged,
                onSend = onSend,
            )
            InterphoneArea.Butler -> ButlerArea(
                state = state,
                onRefresh = onRefreshManifest,
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
            InterphoneArea.Version -> VersionArea(
                appVersion = appVersion,
                buildDate = buildDate,
                buildType = buildType,
            )
        }
    }
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
            .padding(20.dp),
    ) {
        Text(
            text = "Talking to",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = state.targetButlerName.trim().ifEmpty { "Butler Core" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (state.targetButlerName.isBlank()) {
                "Core-facing path"
            } else {
                "Explicit Butler route through Bifröst and Midgard"
            },
            style = MaterialTheme.typography.bodySmall,
        )

        state.sourceButlerName?.let { source ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last response from $source",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            label = { Text("Message") },
            minLines = 3,
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open Config to complete the Bifröst connection.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.phase == RequestPhase.Sending) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.requestId?.let { requestId ->
            Spacer(modifier = Modifier.height(20.dp))
            DiagnosticCard(
                requestId = requestId,
                sourceButlerName = state.sourceButlerName,
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
                label = { Text("Output") },
                minLines = 3,
                supportingText = { Text("Long-press to select and copy") },
            )
        }
    }
}

@Composable
private fun DiagnosticCard(
    requestId: String,
    sourceButlerName: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text = "Request diagnostics",
                style = MaterialTheme.typography.titleSmall,
            )
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Butler on Bifröst",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Live runtime-supplied self-description",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            TextButton(
                onClick = onRefresh,
                enabled = (
                    !state.manifestLoading
                        && isConnectionConfigured(state)
                ),
            ) {
                Text("Refresh")
            }
        }

        if (state.manifestLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.manifestError?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val manifest = state.nodeManifest
        if (manifest == null && !state.manifestLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No node manifest loaded.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        manifest ?: return@Column

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Bifröst",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Version ${manifest.bifrostVersion}")
                Text("Protocol ${manifest.protocolVersion}")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Butler Core",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Version ${manifest.core.version}")

                if (manifest.core.plugins.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    manifest.core.plugins.forEach { plugin ->
                        PluginBlock(plugin)
                    }
                }
            }
        }

        manifest.butlers.forEach { butler ->
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = butler.canonicalName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (butler.available) "Available" else "Unavailable",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (butler.aliases.isNotEmpty()) {
                        Text(
                            text = "Aliases: ${butler.aliases.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    butler.version?.let {
                        Text("Butler version $it")
                    }
                    butler.asgardVersion?.let {
                        Text("Asgard version $it")
                    }
                    if (butler.description.isNotBlank()) {
                        Text(butler.description)
                    }

                    if (butler.entities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Entities",
                            fontWeight = FontWeight.SemiBold,
                        )
                        butler.entities.forEach { entity ->
                            EntityBlock(entity)
                        }
                    }

                    if (butler.plugins.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Butler-local plugins",
                            fontWeight = FontWeight.SemiBold,
                        )
                        butler.plugins.forEach { plugin ->
                            PluginBlock(plugin)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginBlock(
    plugin: ManifestPlugin,
) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Text(
            text = "${plugin.name} • ${plugin.version}",
            fontWeight = FontWeight.Medium,
        )
        if (plugin.description.isNotBlank()) {
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = if (plugin.available) "Available" else "Unavailable",
            style = MaterialTheme.typography.bodySmall,
        )
        ReadinessBlock(plugin.readiness)
        DependenciesBlock(plugin.dependencies)
    }
}

@Composable
private fun EntityBlock(
    entity: ManifestEntity,
) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Text(
            text = entity.name,
            fontWeight = FontWeight.Medium,
        )
        if (entity.description.isNotBlank()) {
            Text(
                text = entity.description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = if (entity.available) "Available" else "Unavailable",
            style = MaterialTheme.typography.bodySmall,
        )
        ReadinessBlock(entity.readiness)
        DependenciesBlock(entity.dependencies)

        entity.methods.forEach { method ->
            Text(
                text = "• ${method.name}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (method.description.isNotBlank()) {
                Text(
                    text = method.description,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ReadinessBlock(method.readiness)
            DependenciesBlock(method.dependencies)
        }
    }
}

@Composable
private fun ReadinessBlock(
    readiness: ManifestReadiness?,
) {
    readiness ?: return
    Text(
        text = buildString {
            append("Readiness: ")
            append(readiness.state)
            readiness.reasonCode?.let {
                append(" • ")
                append(it)
            }
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun DependenciesBlock(
    dependencies: List<ManifestDependency>,
) {
    if (dependencies.isEmpty()) {
        return
    }

    Text(
        text = "Dependencies: " + dependencies.joinToString { dependency ->
            dependency.version?.let {
                "${dependency.name} $it"
            } ?: dependency.name
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun VersionArea(
    appVersion: String,
    buildDate: String,
    buildType: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            text = "Butler Interphone",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Version $appVersion")
        Text("Build date $buildDate")
        Text("Build type $buildType")
    }
}

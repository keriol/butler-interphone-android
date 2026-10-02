package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    val navLabel: String,
    val primary: Boolean = true,
) {
    Text("Text to your Butler", "Text"),
    Voice("Talk to your Butler", "Voice"),
    Butler("Butler on Bifröst", "Runtime"),
    About("About", "About"),
    Config("Connection", "Connection", primary = false),
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

internal enum class ButlerOpeningState {
    None,
    Checking,
    Ready,
    Unavailable,
}

internal fun butlerOpeningState(
    state: InterphoneUiState,
    configuredButlerName: String,
): ButlerOpeningState {
    val butlerName = configuredButlerName.trim()
    if (butlerName.isEmpty()) return ButlerOpeningState.None
    if (!isConnectionConfigured(state)) return ButlerOpeningState.Unavailable
    if (state.directoryLoading) return ButlerOpeningState.Checking
    if (state.directoryError != null) return ButlerOpeningState.Unavailable

    val available = state.availableButlers.any { butler ->
        butler.canonicalName.equals(butlerName, ignoreCase = true)
            || butler.aliases.any { alias ->
                alias.equals(butlerName, ignoreCase = true)
            }
    }
    return if (available) {
        ButlerOpeningState.Ready
    } else {
        ButlerOpeningState.Unavailable
    }
}

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
    val openingButlerName = state.targetButlerName.trim()
    var openingDismissed by rememberSaveable {
        mutableStateOf(false)
    }
    val openingState = butlerOpeningState(
        state = state,
        configuredButlerName = openingButlerName,
    )

    if (!openingDismissed && openingState != ButlerOpeningState.None) {
        ButlerOpeningExperience(
            butlerName = openingButlerName,
            openingState = openingState,
            onContinue = { openingDismissed = true },
            onOpenConnection = {
                openingDismissed = true
                areaName = InterphoneArea.Config.name
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
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
                    Text(
                        if (isConnectionConfigured(state)) {
                            "Connected"
                        } else {
                            "Connection"
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
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
                    onOpenConnection = {
                        areaName = InterphoneArea.Config.name
                    },
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

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            InterphoneArea.entries
                .filter { it.primary }
                .forEach { candidate ->
                    NavigationBarItem(
                        selected = candidate == area,
                        onClick = { areaName = candidate.name },
                        icon = {},
                        label = { Text(candidate.navLabel) },
                    )
                }
        }
    }
}

@Composable
private fun ButlerOpeningExperience(
    butlerName: String,
    openingState: ButlerOpeningState,
    onContinue: () -> Unit,
    onOpenConnection: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(88.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            when (openingState) {
                ButlerOpeningState.Checking -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(
                            R.string.butler_checking,
                            butlerName,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }

                ButlerOpeningState.Ready -> {
                    Text(
                        text = stringResource(
                            R.string.butler_greeting_title,
                            butlerName,
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.butler_greeting_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                R.string.butler_greeting_continue,
                                butlerName,
                            )
                        )
                    }
                }

                ButlerOpeningState.Unavailable -> {
                    Text(
                        text = stringResource(
                            R.string.butler_unavailable_title,
                            butlerName,
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.butler_unavailable_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    OutlinedButton(
                        onClick = onOpenConnection,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.butler_open_connection))
                    }
                }

                ButlerOpeningState.None -> Unit
            }
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = talkTargetLabel(state.targetButlerName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = talkRouteLabel(state.targetButlerName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.sourceButlerName?.let { source ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last response from $source",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phase != RequestPhase.Sending,
            label = { Text("Message") },
            placeholder = {
                Text(
                    stringResource(
                        R.string.talk_message_hint,
                        talkTargetLabel(state.targetButlerName),
                    )
                )
            },
            minLines = 2,
            shape = MaterialTheme.shapes.large,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
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

        state.response?.let { value ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.talk_output_label,
                            state.sourceButlerName
                                ?: talkTargetLabel(state.targetButlerName),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Long-press to select and copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.error?.let { value ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Request failed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
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

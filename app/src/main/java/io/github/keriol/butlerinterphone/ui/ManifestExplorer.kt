package io.github.keriol.butlerinterphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.keriol.butlerinterphone.client.ManifestCallable
import io.github.keriol.butlerinterphone.client.ManifestDependency
import io.github.keriol.butlerinterphone.client.ManifestEntity
import io.github.keriol.butlerinterphone.client.ManifestPlugin
import io.github.keriol.butlerinterphone.client.ManifestReadiness
import io.github.keriol.butlerinterphone.client.NodeManifest

internal enum class RuntimeStatusFilter(
    val label: String,
) {
    All("All"),
    Usable("Usable"),
    Degraded("Degraded"),
    Unavailable("Unavailable"),
    Unknown("Unknown"),
}

internal fun runtimeState(
    readiness: ManifestReadiness?,
    available: Boolean,
): String = readiness?.state?.trim()?.lowercase()?.ifEmpty { null }
    ?: if (available) "not_reported" else "unavailable"

internal fun matchesRuntimeStatus(
    readiness: ManifestReadiness?,
    available: Boolean,
    filter: RuntimeStatusFilter,
): Boolean {
    if (filter == RuntimeStatusFilter.All) return true

    val state = runtimeState(readiness, available)
    return when (filter) {
        RuntimeStatusFilter.All -> true
        RuntimeStatusFilter.Usable -> state == "usable"
        RuntimeStatusFilter.Degraded -> state == "degraded"
        RuntimeStatusFilter.Unavailable -> state in setOf("unavailable", "error")
        RuntimeStatusFilter.Unknown -> state in setOf("unknown", "not_reported")
    }
}

private fun containsQuery(
    query: String,
    vararg values: String?,
): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return values.any { value ->
        value?.lowercase()?.contains(needle) == true
    }
}

internal fun pluginMatchesExplorer(
    plugin: ManifestPlugin,
    query: String,
    filter: RuntimeStatusFilter,
): Boolean = containsQuery(
    query,
    plugin.name,
    plugin.version,
    plugin.description,
    plugin.readiness?.state,
    plugin.readiness?.reasonCode,
) && matchesRuntimeStatus(plugin.readiness, plugin.available, filter)

internal fun callableMatchesExplorer(
    callable: ManifestCallable,
    query: String,
    filter: RuntimeStatusFilter,
): Boolean = containsQuery(
    query,
    callable.name,
    callable.description,
    callable.readiness?.state,
    callable.readiness?.reasonCode,
) && matchesRuntimeStatus(callable.readiness, callable.available, filter)

internal fun entityMatchesExplorer(
    entity: ManifestEntity,
    query: String,
    filter: RuntimeStatusFilter,
): Boolean {
    val entityMatch = containsQuery(
        query,
        entity.name,
        entity.description,
        entity.readiness?.state,
        entity.readiness?.reasonCode,
    ) && matchesRuntimeStatus(entity.readiness, entity.available, filter)

    return entityMatch || entity.methods.any {
        callableMatchesExplorer(it, query, filter)
    }
}

@Composable
internal fun ButlerRuntimeExplorer(
    manifest: NodeManifest,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable {
        mutableStateOf(RuntimeStatusFilter.All.name)
    }
    val filter = RuntimeStatusFilter.valueOf(filterName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Runtime explorer",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Live manifest from Bifröst",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(
                onClick = onRefresh,
                enabled = refreshEnabled,
            ) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search runtime") },
            placeholder = { Text("printer_status, Rosie, media…") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RuntimeStatusFilter.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == filter,
                    onClick = { filterName = candidate.name },
                    label = { Text(candidate.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CompactNodeCard(
            title = "Bifröst",
            subtitle = "v${manifest.bifrostVersion} • protocol ${manifest.protocolVersion}",
        )

        Spacer(modifier = Modifier.height(10.dp))

        CoreTree(
            manifest = manifest,
            query = query,
            filter = filter,
        )

        manifest.butlers.forEach { butler ->
            val visibleEntities = butler.entities.filter {
                entityMatchesExplorer(it, query, filter)
            }
            val visiblePlugins = butler.plugins.filter {
                pluginMatchesExplorer(it, query, filter)
            }
            val butlerTextMatch = containsQuery(
                query,
                butler.canonicalName,
                butler.description,
                butler.version,
                butler.asgardVersion,
                butler.aliases.joinToString(" "),
            )
            val butlerStatusMatch = when (filter) {
                RuntimeStatusFilter.All -> true
                RuntimeStatusFilter.Usable -> butler.available
                RuntimeStatusFilter.Unavailable -> !butler.available
                RuntimeStatusFilter.Degraded,
                RuntimeStatusFilter.Unknown -> false
            }

            if (
                (butlerTextMatch && butlerStatusMatch)
                || visibleEntities.isNotEmpty()
                || visiblePlugins.isNotEmpty()
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                ExpandableSection(
                    title = butler.canonicalName,
                    statusText = if (butler.available) "available" else "unavailable",
                    forceExpanded = query.isNotBlank() || filter != RuntimeStatusFilter.All,
                ) {
                    butler.version?.let { Text("Butler version $it") }
                    butler.asgardVersion?.let { Text("Asgard version $it") }
                    if (butler.aliases.isNotEmpty()) {
                        Text(
                            "Aliases: ${butler.aliases.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (butler.description.isNotBlank()) {
                        Text(
                            butler.description,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (visibleEntities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Entities",
                            fontWeight = FontWeight.SemiBold,
                        )
                        visibleEntities.forEach { entity ->
                            EntityTree(
                                entity = entity,
                                query = query,
                                filter = filter,
                            )
                        }
                    }

                    if (visiblePlugins.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Butler-local plugins",
                            fontWeight = FontWeight.SemiBold,
                        )
                        visiblePlugins.forEach { plugin ->
                            PluginTree(plugin)
                        }
                    }
                }
            }
        }

        val anyVisible = manifest.core.plugins.any {
            pluginMatchesExplorer(it, query, filter)
        } || manifest.butlers.any { butler ->
            butler.entities.any { entityMatchesExplorer(it, query, filter) }
                || butler.plugins.any { pluginMatchesExplorer(it, query, filter) }
                || (containsQuery(query, butler.canonicalName, butler.description)
                    && when (filter) {
                        RuntimeStatusFilter.All -> true
                        RuntimeStatusFilter.Usable -> butler.available
                        RuntimeStatusFilter.Unavailable -> !butler.available
                        else -> false
                    })
        }

        if (!anyVisible && (query.isNotBlank() || filter != RuntimeStatusFilter.All)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No runtime entries match the current search/filter.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CoreTree(
    manifest: NodeManifest,
    query: String,
    filter: RuntimeStatusFilter,
) {
    val plugins = manifest.core.plugins.filter {
        pluginMatchesExplorer(it, query, filter)
    }
    val coreMatches = containsQuery(query, "Butler Core", manifest.core.version)

    if (!coreMatches && plugins.isEmpty()) return

    ExpandableSection(
        title = "Butler Core",
        statusText = null,
        forceExpanded = query.isNotBlank() || filter != RuntimeStatusFilter.All,
    ) {
        Text("Version ${manifest.core.version}")
        if (plugins.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            plugins.forEach { PluginTree(it) }
        }
    }
}

@Composable
private fun EntityTree(
    entity: ManifestEntity,
    query: String,
    filter: RuntimeStatusFilter,
) {
    val entityQueryMatch = containsQuery(
        query,
        entity.name,
        entity.description,
        entity.readiness?.state,
        entity.readiness?.reasonCode,
    )

    val methods = entity.methods.filter { method ->
        if (entityQueryMatch) {
            matchesRuntimeStatus(method.readiness, method.available, filter)
        } else {
            callableMatchesExplorer(method, query, filter)
        }
    }

    ExpandableSection(
        title = entity.name,
        statusText = runtimeStatusLabel(entity.readiness, entity.available),
        forceExpanded = query.isNotBlank() || filter != RuntimeStatusFilter.All,
    ) {
        if (entity.description.isNotBlank()) {
            Text(
                entity.description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ReadinessDetails(entity.readiness)
        DependenciesDetails(entity.dependencies)

        methods.forEach { method ->
            ExpandableSection(
                title = method.name,
                statusText = runtimeStatusLabel(method.readiness, method.available),
                forceExpanded = query.isNotBlank(),
                compact = true,
            ) {
                if (method.description.isNotBlank()) {
                    Text(
                        method.description,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ReadinessDetails(method.readiness)
                DependenciesDetails(method.dependencies)
            }
        }
    }
}

@Composable
private fun PluginTree(plugin: ManifestPlugin) {
    ExpandableSection(
        title = plugin.name,
        statusText = runtimeStatusLabel(plugin.readiness, plugin.available),
        compact = true,
    ) {
        Text(
            "Version ${plugin.version}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (plugin.description.isNotBlank()) {
            Text(
                plugin.description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ReadinessDetails(plugin.readiness)
        DependenciesDetails(plugin.dependencies)
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    statusText: String?,
    forceExpanded: Boolean = false,
    compact: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val open = expanded || forceExpanded

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 3.dp else 5.dp)
            .clickable { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = (if (open) "▾ " else "▸ ") + title,
                    fontWeight = FontWeight.SemiBold,
                )
                statusText?.let { RuntimeStatusChip(it) }
            }

            if (open) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun CompactNodeCard(
    title: String,
    subtitle: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RuntimeStatusChip(status: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
    ) {
        Text(
            text = status.replace('_', ' '),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun runtimeStatusLabel(
    readiness: ManifestReadiness?,
    available: Boolean,
): String = when (val state = runtimeState(readiness, available)) {
    "not_reported" -> "not reported"
    else -> state
}

@Composable
private fun ReadinessDetails(readiness: ManifestReadiness?) {
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
private fun DependenciesDetails(
    dependencies: List<ManifestDependency>,
) {
    if (dependencies.isEmpty()) return

    Text(
        text = "Dependencies: " + dependencies.joinToString { dependency ->
            dependency.version?.let { "${dependency.name} $it" }
                ?: dependency.name
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.ManifestCallable
import io.github.keriol.butlerinterphone.client.ManifestEntity
import io.github.keriol.butlerinterphone.client.ManifestPlugin
import io.github.keriol.butlerinterphone.client.ManifestReadiness
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestExplorerStateTest {
    @Test
    fun searchFindsCapabilityInsideEntityHierarchy() {
        val entity = ManifestEntity(
            name = "appliance",
            methods = listOf(
                ManifestCallable(
                    name = "printer_status",
                    description = "Printer state",
                    readiness = ManifestReadiness("usable"),
                )
            ),
            readiness = ManifestReadiness("usable"),
        )

        assertTrue(
            entityMatchesExplorer(
                entity,
                "printer_status",
                RuntimeStatusFilter.All,
            )
        )
        assertFalse(
            entityMatchesExplorer(
                entity,
                "media_policy",
                RuntimeStatusFilter.All,
            )
        )
    }

    @Test
    fun statusFilterDistinguishesUnknownFromUnavailable() {
        assertTrue(
            matchesRuntimeStatus(
                ManifestReadiness("unknown", "probe_not_declared"),
                available = true,
                filter = RuntimeStatusFilter.Unknown,
            )
        )
        assertFalse(
            matchesRuntimeStatus(
                ManifestReadiness("unknown", "probe_not_declared"),
                available = true,
                filter = RuntimeStatusFilter.Unavailable,
            )
        )
        assertTrue(
            matchesRuntimeStatus(
                ManifestReadiness("unavailable"),
                available = false,
                filter = RuntimeStatusFilter.Unavailable,
            )
        )
    }

    @Test
    fun missingReadinessIsSearchableAsUnknownButDisplayedSeparately() {
        assertTrue(
            matchesRuntimeStatus(
                readiness = null,
                available = true,
                filter = RuntimeStatusFilter.Unknown,
            )
        )
        assertTrue(runtimeState(null, true) == "not_reported")
    }

    @Test
    fun pluginSearchIncludesDescriptionAndReadinessReason() {
        val plugin = ManifestPlugin(
            name = "Home Assistant Plugin",
            version = "0.2.0.dev0",
            description = "Physical orchestration bridge",
            readiness = ManifestReadiness(
                state = "degraded",
                reasonCode = "provider_slow",
            ),
        )

        assertTrue(
            pluginMatchesExplorer(
                plugin,
                "orchestration",
                RuntimeStatusFilter.All,
            )
        )
        assertTrue(
            pluginMatchesExplorer(
                plugin,
                "provider_slow",
                RuntimeStatusFilter.Degraded,
            )
        )
    }
}

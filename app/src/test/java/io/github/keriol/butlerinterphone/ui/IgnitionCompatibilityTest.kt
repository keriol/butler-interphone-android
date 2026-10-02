package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.ManifestButler
import io.github.keriol.butlerinterphone.client.ManifestCore
import io.github.keriol.butlerinterphone.client.ManifestPlugin
import io.github.keriol.butlerinterphone.client.NodeManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IgnitionCompatibilityTest {

    @Test
    fun exactIgnitionBaselineIsCompatible() {
        val report = IgnitionCompatibility.evaluate(
            manifest(
                bifrost = "0.1.0",
                core = "0.3.0",
                midgard = "0.1.0",
                hap = "0.3.0",
                alfred = "0.5.0",
            )
        )

        assertNull(report.networkError)
        assertTrue(report.warnings.isEmpty())
        assertNull(IgnitionCompatibility.targetError(report, "Alfred"))
    }

    @Test
    fun prereleaseAtSameNumericVersionIsOlderThanStableFloor() {
        val report = IgnitionCompatibility.evaluate(
            manifest(bifrost = "0.1.0.dev0")
        )

        assertTrue(report.networkError!!.contains("Bifröst 0.1.0.dev0"))
    }

    @Test
    fun oldNetworkFoundationBlocksNode() {
        val report = IgnitionCompatibility.evaluate(
            manifest(
                bifrost = "0.0.9",
                core = "0.2.9",
                midgard = "0.0.9",
            )
        )

        assertTrue(report.networkError!!.contains("Bifröst 0.0.9"))
        assertTrue(report.networkError!!.contains("Butler Core 0.2.9"))
        assertTrue(report.networkError!!.contains("Midgard 0.0.9"))
    }

    @Test
    fun missingMidgardIsNetworkBlocker() {
        val report = IgnitionCompatibility.evaluate(
            manifest(midgard = null)
        )

        assertTrue(report.networkError!!.contains("Midgard version is unavailable"))
    }

    @Test
    fun oldHapIsScopedWarningNotNetworkBlocker() {
        val report = IgnitionCompatibility.evaluate(
            manifest(hap = "0.2.0")
        )

        assertNull(report.networkError)
        assertEquals(1, report.warnings.size)
        assertTrue(report.warnings.single().contains("Home Assistant Plugin 0.2.0"))
    }

    @Test
    fun oldAlfredBlocksOnlyAlfredAndAliases() {
        val report = IgnitionCompatibility.evaluate(
            manifest(alfred = "0.4.9")
        )

        assertNull(report.networkError)
        assertTrue(report.warnings.isEmpty())
        assertTrue(
            IgnitionCompatibility.targetError(report, "Alfred")!!
                .contains("Alfred 0.4.9")
        )
        assertTrue(
            IgnitionCompatibility.targetError(report, "Alf")!!
                .contains("Alfred 0.4.9")
        )
        assertNull(IgnitionCompatibility.targetError(report, null))
    }

    @Test
    fun unsupportedProtocolBlocksNode() {
        val report = IgnitionCompatibility.evaluate(
            manifest(protocol = 2)
        )

        assertTrue(report.networkError!!.contains("Protocol 2 is unsupported"))
    }

    private fun manifest(
        protocol: Int = 1,
        bifrost: String = "0.1.0",
        core: String = "0.3.0",
        midgard: String? = "0.1.0",
        hap: String? = "0.3.0",
        alfred: String? = "0.5.0",
    ): NodeManifest {
        val plugins = buildList {
            midgard?.let {
                add(ManifestPlugin(name = "Midgard", version = it))
            }
            hap?.let {
                add(ManifestPlugin(name = "Home Assistant Plugin", version = it))
            }
        }
        return NodeManifest(
            protocolVersion = protocol,
            bifrostVersion = bifrost,
            core = ManifestCore(
                version = core,
                plugins = plugins,
            ),
            butlers = listOf(
                ManifestButler(
                    canonicalName = "Alfred",
                    aliases = listOf("Alf"),
                    version = alfred,
                )
            ),
        )
    }
}

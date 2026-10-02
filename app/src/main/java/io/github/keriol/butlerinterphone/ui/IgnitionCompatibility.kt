package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.ManifestPlugin
import io.github.keriol.butlerinterphone.client.NodeManifest

internal object IgnitionCompatibility {
    const val PROTOCOL_VERSION = 1
    const val MIN_BIFROST = "0.1.0"
    const val MIN_CORE = "0.3.0"
    const val MIN_MIDGARD = "0.1.0"
    const val MIN_HAP = "0.3.0"
    const val MIN_ALFRED = "0.5.0"

    fun evaluate(manifest: NodeManifest): CompatibilityReport {
        val networkIssues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val incompatibleButlers = linkedMapOf<String, String>()

        if (manifest.protocolVersion != PROTOCOL_VERSION) {
            networkIssues += (
                "Protocol ${manifest.protocolVersion} is unsupported. " +
                    "Required: $PROTOCOL_VERSION."
            )
        }

        requireAtLeast(
            component = "Bifröst",
            actual = manifest.bifrostVersion,
            required = MIN_BIFROST,
        )?.let(networkIssues::add)

        requireAtLeast(
            component = "Butler Core",
            actual = manifest.core.version,
            required = MIN_CORE,
        )?.let(networkIssues::add)

        val midgard = manifest.core.plugins.firstNamed("Midgard")
        if (midgard == null) {
            networkIssues += "Midgard version is unavailable. Required: >= $MIN_MIDGARD."
        } else {
            requireAtLeast(
                component = "Midgard",
                actual = midgard.version,
                required = MIN_MIDGARD,
            )?.let(networkIssues::add)
        }

        val hap = manifest.core.plugins.firstNamed("Home Assistant Plugin")
        if (hap == null) {
            warnings += (
                "Home Assistant capability compatibility is unverified: " +
                    "HAP version is unavailable (required >= $MIN_HAP)."
            )
        } else {
            requireAtLeast(
                component = "Home Assistant Plugin",
                actual = hap.version,
                required = MIN_HAP,
            )?.let { warnings += it }
        }

        manifest.butlers.forEach { butler ->
            if (butler.canonicalName.equals("Alfred", ignoreCase = true)) {
                val message = requireAtLeast(
                    component = "Alfred",
                    actual = butler.version,
                    required = MIN_ALFRED,
                )
                if (message != null) {
                    (listOf(butler.canonicalName) + butler.aliases)
                        .filter { it.isNotBlank() }
                        .forEach { name ->
                            incompatibleButlers[name.lowercase()] = message
                        }
                }
            }
        }

        return CompatibilityReport(
            networkError = networkIssues
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = " "),
            warnings = warnings,
            incompatibleButlers = incompatibleButlers,
        )
    }

    fun targetError(
        report: CompatibilityReport,
        targetButlerName: String?,
    ): String? {
        val target = targetButlerName?.trim().orEmpty()
        if (target.isEmpty()) return null
        return report.incompatibleButlers[target.lowercase()]
    }

    private fun List<ManifestPlugin>.firstNamed(name: String): ManifestPlugin? =
        firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun requireAtLeast(
        component: String,
        actual: String?,
        required: String,
    ): String? {
        val actualValue = actual?.trim().orEmpty()
        if (actualValue.isEmpty()) {
            return "$component version is unavailable. Required: >= $required."
        }

        val actualVersion = SemanticVersion.parse(actualValue)
            ?: return "$component version '$actualValue' is not understood. Required: >= $required."
        val requiredVersion = SemanticVersion.parse(required)
            ?: error("Invalid built-in compatibility version: $required")

        return if (actualVersion < requiredVersion) {
            "$component $actualValue is incompatible. Required: >= $required."
        } else {
            null
        }
    }
}

internal data class CompatibilityReport(
    val networkError: String? = null,
    val warnings: List<String> = emptyList(),
    val incompatibleButlers: Map<String, String> = emptyMap(),
)

internal data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: Boolean,
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }

        return when {
            prerelease == other.prerelease -> 0
            prerelease -> -1
            else -> 1
        }
    }

    companion object {
        private val pattern = Regex(
            """^v?(\d+)\.(\d+)\.(\d+)(.*)$""",
            RegexOption.IGNORE_CASE,
        )

        fun parse(value: String): SemanticVersion? {
            val match = pattern.matchEntire(value.trim()) ?: return null
            return SemanticVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
                prerelease = match.groupValues[4].isNotBlank(),
            )
        }
    }
}

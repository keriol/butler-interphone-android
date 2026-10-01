package io.github.keriol.butlerinterphone.client

data class InterphoneRequest(
    val requestId: String,
    val message: String,
    val targetButlerName: String? = null,
)

data class InterphoneResponse(
    val requestId: String,
    val response: String,
    val sourceButlerName: String? = null,
)

data class ButlerDirectoryEntry(
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val available: Boolean = true,
)

data class ManifestReadiness(
    val state: String,
    val reasonCode: String? = null,
)

data class ManifestDependency(
    val name: String,
    val version: String? = null,
)

data class ManifestCallable(
    val name: String,
    val description: String = "",
    val available: Boolean = true,
    val readiness: ManifestReadiness? = null,
    val dependencies: List<ManifestDependency> = emptyList(),
)

data class ManifestEntity(
    val name: String,
    val description: String = "",
    val available: Boolean = true,
    val readiness: ManifestReadiness? = null,
    val methods: List<ManifestCallable> = emptyList(),
    val dependencies: List<ManifestDependency> = emptyList(),
)

data class ManifestPlugin(
    val name: String,
    val version: String,
    val description: String = "",
    val available: Boolean = true,
    val readiness: ManifestReadiness? = null,
    val dependencies: List<ManifestDependency> = emptyList(),
)

data class ManifestButler(
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val description: String = "",
    val version: String? = null,
    val available: Boolean = true,
    val asgardVersion: String? = null,
    val entities: List<ManifestEntity> = emptyList(),
    val plugins: List<ManifestPlugin> = emptyList(),
)

data class ManifestCore(
    val version: String,
    val plugins: List<ManifestPlugin> = emptyList(),
)

data class NodeManifest(
    val protocolVersion: Int,
    val bifrostVersion: String,
    val core: ManifestCore,
    val butlers: List<ManifestButler> = emptyList(),
)

interface InterphoneClient {
    suspend fun send(
        request: InterphoneRequest,
    ): InterphoneResponse

    suspend fun listButlers(): List<ButlerDirectoryEntry> = emptyList()

    suspend fun getNodeManifest(): NodeManifest? = null
}

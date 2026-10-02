package io.github.keriol.butlerinterphone.branding

data class ProjectLink(
    val label: String,
    val url: String,
)

data class InterphoneBranding(
    val appName: String,
    val tagline: String,
    val description: String,
    val projectName: String,
    val projectUrl: String,
    val repositories: List<ProjectLink>,
    val supportLabel: String,
    val supportUrl: String,
    val license: String,
)

val DefaultInterphoneBranding = InterphoneBranding(
    appName = "Butler Interphone",
    tagline = "Your doorway into the Butler ecosystem.",
    description = "The Android client for the Butler ecosystem. Interphone talks to Butler runtimes through the Bifröst client boundary.",
    projectName = "Wilfred",
    projectUrl = "https://github.com/keriol/butler-wilfred",
    repositories = listOf(
        ProjectLink(
            label = "Butler Interphone",
            url = "https://github.com/keriol/butler-interphone-android",
        ),
        ProjectLink(
            label = "Butler Core",
            url = "https://github.com/keriol/butler-core",
        ),
        ProjectLink(
            label = "Bifröst",
            url = "https://github.com/keriol/Butler-Core-Bifrost-Plugin",
        ),
        ProjectLink(
            label = "Midgard",
            url = "https://github.com/keriol/butler-core-midgard-plugin",
        ),
        ProjectLink(
            label = "Wilfred",
            url = "https://github.com/keriol/butler-wilfred",
        ),
    ),
    supportLabel = "Support Wilfred on Ko-fi",
    supportUrl = "https://ko-fi.com/butlerwilfred",
    license = "Apache License 2.0",
)

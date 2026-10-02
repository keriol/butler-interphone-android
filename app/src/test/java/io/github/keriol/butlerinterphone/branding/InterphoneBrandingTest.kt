package io.github.keriol.butlerinterphone.branding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterphoneBrandingTest {
    @Test
    fun defaultBrandingKeepsReleaseIdentityCentralized() {
        val branding = DefaultInterphoneBranding

        assertEquals("Butler Interphone", branding.appName)
        assertTrue(branding.tagline.isNotBlank())
        assertTrue(branding.description.isNotBlank())
        assertEquals("Apache License 2.0", branding.license)
    }

    @Test
    fun defaultBrandingUsesOnlyHttpsProjectLinks() {
        val branding = DefaultInterphoneBranding
        val urls = buildList {
            add(branding.projectUrl)
            add(branding.supportUrl)
            addAll(branding.repositories.map { it.url })
        }

        assertTrue(urls.isNotEmpty())
        assertTrue(urls.all { it.startsWith("https://") })
        assertTrue(branding.repositories.map { it.label }.toSet().size == branding.repositories.size)
    }
}

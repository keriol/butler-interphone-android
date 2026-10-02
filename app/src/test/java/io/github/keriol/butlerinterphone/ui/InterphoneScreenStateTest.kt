package io.github.keriol.butlerinterphone.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterphoneScreenStateTest {
    @Test
    fun connectionIsConfiguredWhenAllRequiredFieldsArePresent() {
        assertTrue(
            isConnectionConfigured(
                InterphoneUiState(
                    protocol = "http",
                    host = "example.local",
                    port = "5055",
                    token = "token",
                )
            )
        )
    }

    @Test
    fun connectionIsNotConfiguredWhenAnyRequiredFieldIsMissing() {
        assertFalse(
            isConnectionConfigured(
                InterphoneUiState(
                    protocol = "http",
                    host = "example.local",
                    port = "5055",
                    token = "",
                )
            )
        )
    }
    @Test
    fun coreRouteUsesCompactCoreLabels() {
        assertTrue(talkTargetLabel("") == "Butler Core")
        assertTrue(talkRouteLabel("") == "Core-facing path")
    }

    @Test
    fun explicitButlerRouteUsesCompactMidgardLabel() {
        assertTrue(talkTargetLabel("  Alfred  ") == "Alfred")
        assertTrue(talkRouteLabel("Alfred") == "Via Bifröst and Midgard")
    }

}

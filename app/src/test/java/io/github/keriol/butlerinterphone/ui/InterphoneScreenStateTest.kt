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
    fun configuredDefaultButlerWaitsForDirectoryAndThenGreetsWhenAvailable() {
        val checking = InterphoneUiState(
            protocol = "http",
            host = "example.local",
            port = "5055",
            token = "token",
            targetButlerName = "Alfred",
            directoryLoading = true,
        )
        assertTrue(
            butlerOpeningState(checking, "Alfred")
                == ButlerOpeningState.Checking
        )

        val ready = checking.copy(
            directoryLoading = false,
            availableButlers = listOf(
                io.github.keriol.butlerinterphone.client.ButlerDirectoryEntry(
                    canonicalName = "Alfred",
                    available = true,
                )
            ),
        )
        assertTrue(
            butlerOpeningState(ready, "Alfred")
                == ButlerOpeningState.Ready
        )
    }

    @Test
    fun configuredDefaultButlerNeverFallsBackToAnotherAvailableButler() {
        val state = InterphoneUiState(
            protocol = "http",
            host = "example.local",
            port = "5055",
            token = "token",
            targetButlerName = "Alfred",
            availableButlers = listOf(
                io.github.keriol.butlerinterphone.client.ButlerDirectoryEntry(
                    canonicalName = "Wilfred",
                    available = true,
                )
            ),
        )

        assertTrue(
            butlerOpeningState(state, "Alfred")
                == ButlerOpeningState.Unavailable
        )
    }

    @Test
    fun coreFacingModeSkipsPersonalButlerOpening() {
        val state = InterphoneUiState(
            protocol = "http",
            host = "example.local",
            port = "5055",
            token = "token",
        )

        assertTrue(
            butlerOpeningState(state, "")
                == ButlerOpeningState.None
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

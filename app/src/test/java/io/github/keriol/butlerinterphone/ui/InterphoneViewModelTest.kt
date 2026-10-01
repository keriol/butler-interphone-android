package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.client.InterphoneRequest
import io.github.keriol.butlerinterphone.client.InterphoneResponse
import io.github.keriol.butlerinterphone.settings.BifrostConnectionSettings
import io.github.keriol.butlerinterphone.settings.ConnectionSettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterphoneViewModelTest {

    @Test
    fun sendUsesRuntimeSettingsPersistsThemAndPreservesTargetlessCorrelation() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val pending = CompletableDeferred<InterphoneResponse>()
            var capturedEndpoint: String? = null
            var capturedToken: String? = null
            var capturedRequest: InterphoneRequest? = null
            val store = FakeSettingsStore()

            val clientFactory = { endpoint: String, token: String ->
                capturedEndpoint = endpoint
                capturedToken = token

                object : InterphoneClient {
                    override suspend fun send(
                        request: InterphoneRequest,
                    ): InterphoneResponse {
                        capturedRequest = request
                        return pending.await()
                    }
                }
            }

            val viewModel = InterphoneViewModel(
                clientFactory = clientFactory,
                settingsStore = store,
                initialEndpoint = BifrostEndpointParts(
                    protocol = "http",
                    host = "example.test",
                    port = "5055",
                ),
                initialToken = "secret",
                requestIdFactory = { "req-1" },
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()
            runCurrent()

            assertEquals(RequestPhase.Sending, viewModel.uiState.value.phase)
            assertEquals("req-1", viewModel.uiState.value.requestId)
            assertEquals("http://example.test:5055", capturedEndpoint)
            assertEquals("secret", capturedToken)
            assertNull(capturedRequest?.targetButlerName)
            assertEquals(
                BifrostConnectionSettings(
                    protocol = "http",
                    host = "example.test",
                    port = "5055",
                    token = "secret",
                ),
                store.saved,
            )

            pending.complete(
                InterphoneResponse(
                    requestId = "req-1",
                    response = "Ready.",
                )
            )

            advanceUntilIdle()

            assertEquals(RequestPhase.Success, viewModel.uiState.value.phase)
            assertEquals("Ready.", viewModel.uiState.value.response)
            assertNull(viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }


    @Test
    fun explicitButlerTargetIsForwarded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            var capturedRequest: InterphoneRequest? = null

            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ ->
                    object : InterphoneClient {
                        override suspend fun send(
                            request: InterphoneRequest,
                        ): InterphoneResponse {
                            capturedRequest = request
                            return InterphoneResponse(
                                requestId = request.requestId,
                                response = "Ready.",
                                sourceButlerName = "Concrete-Butler",
                            )
                        }
                    }
                },
                settingsStore = FakeSettingsStore(),
                initialEndpoint = BifrostEndpointParts(
                    host = "example.test",
                    port = "5055",
                ),
                initialToken = "secret",
                requestIdFactory = { "req-target" },
            )

            viewModel.onTargetButlerChanged("  Concrete-Butler  ")
            viewModel.onMessageChanged("hello")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(
                "Concrete-Butler",
                capturedRequest?.targetButlerName,
            )
            assertEquals(
                RequestPhase.Success,
                viewModel.uiState.value.phase,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun persistedSettingsOverrideBuildDefaults() {
        val store = FakeSettingsStore(
            loaded = BifrostConnectionSettings(
                protocol = "https",
                host = "saved.test",
                port = "8443",
                token = "saved-token",
            )
        )

        val viewModel = InterphoneViewModel(
            clientFactory = { _, _ -> error("unused") },
            settingsStore = store,
            initialEndpoint = BifrostEndpointParts(
                protocol = "http",
                host = "default.test",
                port = "5055",
            ),
            initialToken = "default-token",
        )

        assertEquals("https", viewModel.uiState.value.protocol)
        assertEquals("saved.test", viewModel.uiState.value.host)
        assertEquals("8443", viewModel.uiState.value.port)
        assertEquals("saved-token", viewModel.uiState.value.token)
    }

    @Test
    fun missingHostIsRejectedWithoutSaving() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val store = FakeSettingsStore()
            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ -> error("client must not be created") },
                settingsStore = store,
                initialToken = "secret",
            )

            viewModel.onPortChanged("5055")
            viewModel.onMessageChanged("hello")
            viewModel.send()

            assertEquals("Bifröst host is required.", viewModel.uiState.value.error)
            assertNull(store.saved)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun invalidPortIsRejected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ -> error("client must not be created") },
                settingsStore = FakeSettingsStore(),
                initialEndpoint = BifrostEndpointParts(
                    host = "example.test",
                    port = "70000",
                ),
                initialToken = "secret",
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()

            assertEquals(
                "Bifröst port must be between 1 and 65535.",
                viewModel.uiState.value.error,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun missingTokenIsRejected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ -> error("client must not be created") },
                settingsStore = FakeSettingsStore(),
                initialEndpoint = BifrostEndpointParts(
                    host = "example.test",
                    port = "5055",
                ),
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()

            assertEquals(
                "Bifröst token is required.",
                viewModel.uiState.value.error,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun blankMessageIsRejectedWithoutCreatingClient() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            var created = false
            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ ->
                    created = true
                    error("client must not be created")
                },
                settingsStore = FakeSettingsStore(),
                initialEndpoint = BifrostEndpointParts(
                    host = "example.test",
                    port = "5055",
                ),
                initialToken = "secret",
            )

            viewModel.onMessageChanged("   ")
            viewModel.send()

            assertEquals(false, created)
            assertEquals(RequestPhase.Error, viewModel.uiState.value.phase)
            assertEquals(
                "Message cannot be empty.",
                viewModel.uiState.value.error,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun correlationMismatchBecomesVisibleError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ ->
                    object : InterphoneClient {
                        override suspend fun send(
                            request: InterphoneRequest,
                        ) = InterphoneResponse(
                            requestId = "wrong-id",
                            response = "wrong response",
                        )
                    }
                },
                settingsStore = FakeSettingsStore(),
                initialEndpoint = BifrostEndpointParts(
                    host = "example.test",
                    port = "5055",
                ),
                initialToken = "secret",
                requestIdFactory = { "req-2" },
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(RequestPhase.Error, viewModel.uiState.value.phase)
            assertEquals(
                "Correlation mismatch.",
                viewModel.uiState.value.error,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeSettingsStore(
        private val loaded: BifrostConnectionSettings? = null,
    ) : ConnectionSettingsStore {
        var saved: BifrostConnectionSettings? = null

        override fun load(): BifrostConnectionSettings? = loaded

        override fun save(settings: BifrostConnectionSettings) {
            saved = settings
        }
    }
}

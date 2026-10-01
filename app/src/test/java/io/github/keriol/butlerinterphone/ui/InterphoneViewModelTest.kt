package io.github.keriol.butlerinterphone.ui

import io.github.keriol.butlerinterphone.client.InterphoneClient
import io.github.keriol.butlerinterphone.client.InterphoneRequest
import io.github.keriol.butlerinterphone.client.InterphoneResponse
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
    fun sendUsesRuntimeSettingsAndPreservesTargetlessCorrelation() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val pending = CompletableDeferred<InterphoneResponse>()
            var capturedEndpoint: String? = null
            var capturedToken: String? = null
            var capturedRequest: InterphoneRequest? = null

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
                initialEndpoint = "http://example.test:5055",
                initialToken = "secret",
                requestIdFactory = { "req-1" },
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()
            runCurrent()

            assertEquals(
                RequestPhase.Sending,
                viewModel.uiState.value.phase,
            )
            assertEquals(
                "req-1",
                viewModel.uiState.value.requestId,
            )
            assertEquals("http://example.test:5055", capturedEndpoint)
            assertEquals("secret", capturedToken)
            assertNull(capturedRequest?.targetButlerName)

            pending.complete(
                InterphoneResponse(
                    requestId = "req-1",
                    response = "Ready.",
                )
            )

            advanceUntilIdle()

            assertEquals(
                RequestPhase.Success,
                viewModel.uiState.value.phase,
            )
            assertEquals(
                "Ready.",
                viewModel.uiState.value.response,
            )
            assertNull(viewModel.uiState.value.error)
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
                initialEndpoint = "http://example.test:5055",
                initialToken = "secret",
                requestIdFactory = { "req-blank" },
            )

            viewModel.onMessageChanged("   ")
            viewModel.send()

            assertEquals(false, created)
            assertEquals(
                RequestPhase.Error,
                viewModel.uiState.value.phase,
            )
            assertEquals(
                "Message cannot be empty.",
                viewModel.uiState.value.error,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun missingEndpointIsRejected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val viewModel = InterphoneViewModel(
                clientFactory = { _, _ ->
                    error("client must not be created")
                },
                initialToken = "secret",
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()

            assertEquals(
                "Bifröst endpoint is required.",
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
                clientFactory = { _, _ ->
                    error("client must not be created")
                },
                initialEndpoint = "http://example.test:5055",
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
                initialEndpoint = "http://example.test:5055",
                initialToken = "secret",
                requestIdFactory = { "req-2" },
            )

            viewModel.onMessageChanged("hello")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(
                RequestPhase.Error,
                viewModel.uiState.value.phase,
            )
            assertEquals(
                "Correlation mismatch.",
                viewModel.uiState.value.error,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
}

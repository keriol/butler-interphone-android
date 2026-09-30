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
    fun sendMovesFromSendingToSuccessAndPreservesCorrelation() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val pending = CompletableDeferred<InterphoneResponse>()

            val client = object : InterphoneClient {
                override suspend fun send(
                    request: InterphoneRequest,
                ): InterphoneResponse = pending.await()
            }

            val viewModel = InterphoneViewModel(
                client = client,
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

            pending.complete(
                InterphoneResponse(
                    requestId = "req-1",
                    response = "Echo: hello",
                )
            )

            advanceUntilIdle()

            assertEquals(
                RequestPhase.Success,
                viewModel.uiState.value.phase,
            )
            assertEquals(
                "Echo: hello",
                viewModel.uiState.value.response,
            )
            assertNull(viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun blankMessageIsRejectedWithoutCallingClient() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            var called = false

            val client = object : InterphoneClient {
                override suspend fun send(
                    request: InterphoneRequest,
                ): InterphoneResponse {
                    called = true
                    return InterphoneResponse(
                        requestId = request.requestId,
                        response = "unexpected",
                    )
                }
            }

            val viewModel = InterphoneViewModel(
                client = client,
                requestIdFactory = { "req-blank" },
            )

            viewModel.onMessageChanged("   ")
            viewModel.send()

            assertEquals(false, called)
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
    fun correlationMismatchBecomesVisibleError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val client = object : InterphoneClient {
                override suspend fun send(
                    request: InterphoneRequest,
                ) = InterphoneResponse(
                    requestId = "wrong-id",
                    response = "wrong response",
                )
            }

            val viewModel = InterphoneViewModel(
                client = client,
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

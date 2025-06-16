package com.flashsphere.rainwaveplayer.flow

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import com.flashsphere.rainwaveplayer.BaseTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.test.Test

class FlowAutoRetryTest : BaseTest() {
    @MockK
    private lateinit var connectivityObserverMock: ConnectivityObserver

    @Test
    fun autoRetry_does_not_retry_on_http_4xx() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = HttpException(Response.error<String>(400, "some error".toResponseBody()))

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns true

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers) { errors += it }
        .collect { results += it }

        verify(exactly = 1) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(HttpException::class)
    }

    @Test
    fun autoRetry_does_not_retry_on_http_5xx() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = HttpException(Response.error<String>(500, "some error".toResponseBody()))

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns true

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers) { errors += it }
        .collect { results += it }

        verify(exactly = 1) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(HttpException::class)
    }

    @Test
    fun autoRetry_does_not_retry_on_SSLPeerUnverifiedException_and_has_connectivity() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = SSLPeerUnverifiedException("some reason")

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns true

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers) { errors += it }
        .collect { results += it }

        verify(exactly = 1) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(exception::class)
    }

    @Test
    fun autoRetry_does_not_retry_on_SocketTimeoutException_and_has_connectivity() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = SocketTimeoutException()

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns true

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers) { errors += it }
        .collect { results += it }

        verify(exactly = 1) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(exception::class)
    }

    @Test
    fun autoRetry_does_retry_on_SocketTimeoutException_and_has_no_connectivity() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = SocketTimeoutException()

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns false
        every { connectivityObserverMock.connectivityFlow } returns flowOf(false, true)

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers) { errors += it }
        .collect { results += it }

        verify(exactly = MAX_RETRIES + 1 /* + 1 for original call */) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(11)
        assertThat(errors[0]).isInstanceOf(exception::class)
    }

    @Test
    fun autoRetry_propagates_exception_when_rethrown_in_action() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = RuntimeException()

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns false
        every { connectivityObserverMock.connectivityFlow } returns flowOf(false, true)

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers) { throw it }
        .catch { errors += it }
        .collect { results += it }

        verify(exactly = 1) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(exception::class)
    }

    @Test
    fun autoRetry_propagates_exception_when_no_error_handler_provided() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        val exception = RuntimeException()

        val func = mockk<() -> Unit>()
        every { func() } throws exception

        every { connectivityObserverMock.isConnected() } returns false
        every { connectivityObserverMock.connectivityFlow } returns flowOf(false, true)

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<Int>()

        flow {
            func()
            emit(1)
        }
        .autoRetry(connectivityObserverMock, coroutineDispatchers)
        .catch { errors += it }
        .collect { results += it }

        verify(exactly = 11) { func() }
        assertThat(results).isEmpty()
        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(exception::class)
    }
}

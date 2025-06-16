package com.flashsphere.rainwaveplayer.util

import android.util.Log
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.CrashlyticsTree.Companion.shouldLogException
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.CRASH_REPORTING
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CrashlyticsTreeTest : BaseTest() {
    @MockK
    lateinit var crashlyticsMock: FirebaseCrashlytics

    @Test
    fun shouldLogExceptionWorks() {
        assertThat(shouldLogException(UnknownHostException("www.google.com"))).isFalse()
        assertThat(shouldLogException(RuntimeException(UnknownHostException("www.google.com")))).isFalse()
        assertThat(shouldLogException(SocketTimeoutException("some message"))).isFalse()
        assertThat(shouldLogException(RuntimeException(SocketTimeoutException("some message")))).isFalse()
        assertThat(shouldLogException(ConnectException("some message"))).isFalse()
        assertThat(shouldLogException(RuntimeException(ConnectException("some message")))).isFalse()
        assertThat(shouldLogException(HttpException(Response.error<Any>(502, "body".toResponseBody())))).isFalse()

        assertThat(shouldLogException(RuntimeException("some message"))).isTrue()
        assertThat(shouldLogException(HttpException(Response.error<Any>(500, "body".toResponseBody())))).isTrue()
    }

    @Test
    fun should_log_to_Crashlytics() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        every { dataStoreMock.getBlocking(CRASH_REPORTING) } returns true
        every { dataStoreMock.data } returns emptyFlow()

        val capturedMessage = slot<String>()
        every { crashlyticsMock.log(capture(capturedMessage)) } just runs
        val capturedException = slot<Throwable>()
        every { crashlyticsMock.recordException(capture(capturedException)) } just runs

        val crashlyticsTree = CrashlyticsTree(contextMock, coroutineDispatchers, dataStoreMock)

        val message = "some message"
        val exception = RuntimeException("some exception")
        crashlyticsTree.log(Log.INFO, exception, message)

        coroutineContext.job.children.forEach { it.join() }

        assertThat(capturedMessage.captured).contains(message)
        assertThat(capturedException.captured).isEqualTo(exception)

        verify(exactly = 1) {
            crashlyticsMock.setCrashlyticsCollectionEnabled(true)
            crashlyticsMock.log(any())
            crashlyticsMock.recordException(any())
        }

        confirmVerified(crashlyticsMock)
    }

    @Test
    fun should_not_log_to_Crashlytics_if_not_enabled() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        every { dataStoreMock.getBlocking(CRASH_REPORTING) } returns false
        every { dataStoreMock.data } returns emptyFlow()

        val crashlyticsTree = CrashlyticsTree(contextMock, coroutineDispatchers, dataStoreMock)
        crashlyticsTree.log(Log.INFO, RuntimeException(), "some message")

        coroutineContext.job.children.forEach { it.join() }

        verify(exactly = 1) {
            crashlyticsMock.setCrashlyticsCollectionEnabled(false)
            crashlyticsMock.deleteUnsentReports()
        }

        confirmVerified(crashlyticsMock)
    }

    @Test
    fun should_not_log_to_Crashlytics_for_verbose_and_debug_priority_messages() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(this)

        every { dataStoreMock.getBlocking(CRASH_REPORTING) } returns true
        every { dataStoreMock.data } returns emptyFlow()

        val crashlyticsTree = CrashlyticsTree(contextMock, coroutineDispatchers, dataStoreMock)
        crashlyticsTree.log(Log.DEBUG, RuntimeException(), "some message")

        coroutineContext.job.children.forEach { it.join() }

        verify(exactly = 1) { crashlyticsMock.setCrashlyticsCollectionEnabled(true) }
        verify(exactly = 0) {
            crashlyticsMock.log(any())
            crashlyticsMock.recordException(any())
        }

        confirmVerified(crashlyticsMock)
    }

    override fun setupMocks() {
        super.setupMocks()
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns crashlyticsMock
        every { crashlyticsMock.setCrashlyticsCollectionEnabled(any()) } just runs
        every { crashlyticsMock.deleteUnsentReports() } just runs
    }
}

package com.flashsphere.rainwaveplayer.flow

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.util.readFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import retrofit2.HttpException
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class StationInfoProviderTest : BaseTest() {
    @Test
    fun flow_fetches_station_info_and_repeats_automatically() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        every { userRepositoryMock.getCredentials() } returns null

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<InfoResponse>()

        val currentTime = System.currentTimeMillis().milliseconds.inWholeSeconds
        val json = readFile(this.javaClass, "/json/info-response.json")
            .replaceFirst("1601898553", "$currentTime")
            .replaceFirst("1601898607", "${currentTime + 15}")

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(json))
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
        }

        val stationInfoProvider = createStationInfoProvider()

        stationInfoProvider.flow
            .catch { errors += it }
            .collect { results += it }

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        assertThat(errors).hasSize(1)
        assertThat(errors[0]).isInstanceOf(HttpException::class)
        val exception = errors[0] as HttpException
        assertThat(exception.code()).isEqualTo(400)

        assertThat(results).hasSize(1)
        val infoResponse = results[0]
        assertThat(infoResponse).isNotNull()
        val apiInfo = infoResponse.apiInfo
        assertThat(apiInfo.time).isEqualTo(currentTime)
    }

    @Test
    fun flow_fetches_station_info_and_refreshes_at_next_subscription_when_triggered() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        every { userRepositoryMock.getCredentials() } returns null

        val func = mockk<() -> Unit>()
        every { func() } returns Unit

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<InfoResponse>()

        val currentTime = System.currentTimeMillis().milliseconds.inWholeSeconds
        val json = readFile(this.javaClass, "/json/info-response.json")
            .replaceFirst("1601898553", "$currentTime")
            .replaceFirst("1601898607", "${currentTime + 15}")

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(json))
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(json))
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
        }

        val stationInfoProvider = createStationInfoProvider()

        stationInfoProvider.flow
            .catch { errors += it }
            .collect { results += it }

        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(errors).hasSize(1)
        assertThat(results).hasSize(1)

        errors.clear()
        results.clear()

        stationInfoProvider.refreshAtNextSubscription(5, func)?.join()
        verify(exactly = 1) { func() }

        stationInfoProvider.flow
            .catch { errors += it }
            .collect { results += it }

        assertThat(mockWebServer.requestCount).isEqualTo(4)
        assertThat(errors).hasSize(1)
        assertThat(results).hasSize(2) // previously cached and refresh
    }

    @Test
    fun flow_concurrent_collection_do_not_make_additional_api_calls() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        every { userRepositoryMock.getCredentials() } returns null

        val func = mockk<() -> Unit>()
        every { func() } returns Unit

        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<InfoResponse>()

        val currentTime = System.currentTimeMillis().milliseconds.inWholeSeconds
        val json = readFile(this.javaClass, "/json/info-response.json")
            .replaceFirst("1601898553", "$currentTime")
            .replaceFirst("1601898607", "${currentTime + 15}")

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(json))
            enqueue(MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
        }

        val stationInfoProvider = createStationInfoProvider()

        val job1 = launch {
            stationInfoProvider.flow
                .catch { errors += it }
                .collect { results += it }
        }
        val job2 = launch {
            stationInfoProvider.flow
                .catch { errors += it }
                .collect { results += it }
        }

        job1.join()
        job2.join()

        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(errors).hasSize(2)
        assertThat(results).hasSize(2)
    }

    private fun createStationInfoProvider() = StationInfoProvider(
        rainwaveService = rainwaveService,
        userRepository = userRepositoryMock,
        coroutineDispatchers = coroutineDispatchers,
        stationId = 1,
    )
}

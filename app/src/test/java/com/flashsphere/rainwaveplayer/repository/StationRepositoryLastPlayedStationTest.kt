package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.LAST_PLAYED
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.readFile
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

class StationRepositoryLastPlayedStationTest : BaseTest() {
    @Test
    fun getLastPlayedStationWithoutDefault_returns_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns 5

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getLastPlayedStationWithoutDefault() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()!!
        assertThat(station.id).isEqualTo(5)
        assertThat(station.name).isEqualTo("All")
    }

    @Test
    fun getLastPlayedStationWithoutDefault_returns_no_result_without_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns -1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getLastPlayedStationWithoutDefault() }

        assertThat(mockWebServer.requestCount).isEqualTo(0)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        assertThat(runResult.getOrThrow()).isNull()
    }

    @Test
    fun getLastPlayedStationWithoutDefault_throws_error_when_api_call_fails() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns 1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_BAD_REQUEST))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getLastPlayedStationWithoutDefault() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isTrue()
        assertThat(runResult.isSuccess).isFalse()

        assertThat(runResult.exceptionOrNull()!!).isInstanceOf(HttpException::class)
    }

    @Test
    fun getLastPlayedStationWithDefault_returns_first_station_without_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns -1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getLastPlayedStationWithDefault() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(1)
        assertThat(station.name).isEqualTo("Game")
    }

    @Test
    fun getLastPlayedStationWithDefault_throws_error_when_api_call_fails() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns -1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_BAD_REQUEST))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getLastPlayedStationWithDefault() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isTrue()
        assertThat(runResult.isSuccess).isFalse()

        assertThat(runResult.exceptionOrNull()!!).isInstanceOf(HttpException::class)
    }
}

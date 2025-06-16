package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.ADD_REQUEST_TO_TOP
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.readFile
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit

class StationRepositoryRequestsTest : BaseTest() {
    @Test
    fun requestFave_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/request-fave.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.requestFave(1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ request_favorited_songs_success ]]")
    }

    @Test
    fun requestUnrated_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/request-unrated.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.requestUnrated(1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ request_unrated_songs_success ]]")
    }

    @Test
    fun clearRequests_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/clear-requests.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.clearRequests(1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.getOrThrow().requests).isEmpty()
    }

    @Test
    fun requestSong_returns_result_but_does_not_move_request_to_top_when_not_enabled() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        coEvery { dataStoreMock.get(ADD_REQUEST_TO_TOP) } returns false

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/request-song.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.requestSong(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Requested.")
    }

    @Test
    fun requestSong_returns_result_and_move_request_to_top_when_enabled() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        coEvery { dataStoreMock.get(ADD_REQUEST_TO_TOP) } returns true

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/request-song-two-requests.json")))
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/order-requests.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.requestSong(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        var recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request")

        recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/order_requests")

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Requested.")
    }

    @Test
    fun requestSong_returns_result_and_does_not_move_request_to_top_when_enabled_and_only_one_request() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        coEvery { dataStoreMock.get(ADD_REQUEST_TO_TOP) } returns true

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/request-song.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.requestSong(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Requested.")
    }

    @Test
    fun deleteRequest_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/delete-request.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.deleteRequest(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ request_deleted ]]")
    }

    @Test
    fun pauseRequests_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/pause-requests.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.pauseRequestResponse(1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ radio_requests_paused ]]")
    }

    @Test
    fun resumeRequests_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/resume-requests.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.resumeRequestResponse(1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ radio_requests_unpaused ]]")
    }
}

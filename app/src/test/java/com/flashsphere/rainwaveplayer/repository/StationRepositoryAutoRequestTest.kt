package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.model.request.Request
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.model.user.User
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_CLEAR
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_FAVE
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_UNRATED
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.readFile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit.SECONDS

class StationRepositoryAutoRequestTest : BaseTest() {
    @Test
    fun autoRequest_does_not_run_if_requests_are_paused() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = true)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(0)
        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_does_not_delete_requests_on_cooldown_if_not_enabled() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns false

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(0)
        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_delete_requests_on_cooldown_and_does_not_request_new_songs_if_there_are_remaining_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/delete_request" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/delete-request.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        var recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/delete_request")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/delete_request")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_does_not_request_fave_song_when_delete_requests_on_cooldown_has_an_error() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/delete_request" -> MockResponse()
                    .setResponseCode(HTTP_BAD_REQUEST)
                "/request_favorited_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-fave.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        val recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/delete_request")

        assertThat(runResult.isSuccess).isFalse()
        assertThat(runResult.isFailure).isTrue()
        assertThat(runResult.exceptionOrNull()!!).isInstanceOf(HttpException::class)
    }

    @Test
    fun autoRequest_delete_requests_on_cooldown_and_request_fave_song_if_there_are_no_remaining_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/delete_request" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/delete-request-empty-requests.json"))
                    .setResponseCode(HTTP_OK)
                "/request_favorited_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-fave.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        var recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/delete_request")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_favorited_songs")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_delete_requests_on_cooldown_and_request_unrated_song_if_request_fave_song_return_no_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/delete_request" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/delete-request-empty-requests.json"))
                    .setResponseCode(HTTP_OK)
                "/request_favorited_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-fave-empty-requests.json"))
                    .setResponseCode(HTTP_OK)
                "/request_unrated_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-unrated.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(3)

        var recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/delete_request")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_favorited_songs")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_unrated_songs")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_clear_request_queue_if_all_requests_are_on_cooldown_and_does_not_request_new_songs_if_request_fave_and_unrated_are_disabled() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = true))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns false
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns false
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/clear_requests" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/clear-requests.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        val recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/clear_requests")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_clear_request_queue_if_all_requests_are_on_cooldown_and_request_fave_song() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = true))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/clear_requests" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/clear-requests.json"))
                    .setResponseCode(HTTP_OK)
                "/request_favorited_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-fave.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        var recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/clear_requests")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_favorited_songs")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_clear_request_queue_if_all_requests_are_on_cooldown_and_request_unrated_song_when_request_fave_song_return_no_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = true),
            Request(electionBlocked = false, valid = true, good = true, cool = true))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/clear_requests" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/clear-requests.json"))
                    .setResponseCode(HTTP_OK)
                "/request_favorited_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-fave-empty-requests.json"))
                    .setResponseCode(HTTP_OK)
                "/request_unrated_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-unrated.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(3)

        var recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/clear_requests")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_favorited_songs")

        recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_unrated_songs")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_request_fave_song_when_enabled_and_no_initial_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = emptyList<Request>()
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/request_favorited_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-fave.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        val recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_favorited_songs")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_request_unrated_song_when_enabled_and_no_initial_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = emptyList<Request>()
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns false
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/request_unrated_songs" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/request-unrated.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        val recordedRequest = mockWebServer.takeRequest(1, SECONDS)
        assertThat(recordedRequest).isNotNull()
        assertThat(recordedRequest?.path).isEqualTo("/request_unrated_songs")

        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

    @Test
    fun autoRequest_does_not_request_fave_or_unrated_song_if_there_are_remaining_requests() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        val initialRequests = listOf(Request(electionBlocked = false, valid = true, good = true, cool = false),
            Request(electionBlocked = false, valid = true, good = true, cool = false))
        val infoResponse = mockk<InfoResponse> {
            every { user } returns User(requestsPaused = false)
            every { requests } returns initialRequests
        }

        coEvery { dataStoreMock.get(AUTO_REQUEST_FAVE) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_UNRATED) } returns true
        coEvery { dataStoreMock.get(AUTO_REQUEST_CLEAR) } returns true

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.autoRequestSongs(1, infoResponse) }

        assertThat(mockWebServer.requestCount).isEqualTo(0)
        assertThat(runResult.isSuccess).isTrue()
        assertThat(runResult.isFailure).isFalse()
    }

}

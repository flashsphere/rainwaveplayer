package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class StationRepositoryLibraryTest : BaseTest() {
    @Test
    fun getAllAlbums_populates_cache_and_is_reused_on_subsequent_calls() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/all_albums" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/all-albums.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getAllAlbums(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()

        // cache should be reused from here
        val runResult2 = runCatching { stationRepository.getAllAlbums(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()

        assertThat(runResult1.getOrThrow()).isEqualTo(runResult2.getOrThrow())
        assertThat(runResult1.getOrThrow().albums).hasSize(3)
    }

    @Test
    fun getAllAlbums_populates_cache_and_can_be_forced_refresh() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/all_albums" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/all-albums.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getAllAlbums(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()
        assertThat(runResult1.getOrThrow().albums).hasSize(3)

        // forced refresh will make an api call
        val runResult2 = runCatching { stationRepository.getAllAlbums(1, true) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()
        assertThat(runResult2.getOrThrow().albums).hasSize(3)
    }

    @Test
    fun getAllArtists_populates_cache_and_is_reused_on_subsequent_calls() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/all_artists" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/all-artists.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getAllArtists(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()

        // cache should be reused from here
        val runResult2 = runCatching { stationRepository.getAllArtists(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()

        assertThat(runResult1.getOrThrow()).isEqualTo(runResult2.getOrThrow())
        assertThat(runResult1.getOrThrow().artists).hasSize(3)
    }

    @Test
    fun getAllArtists_populates_cache_and_can_be_forced_refresh() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/all_artists" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/all-artists.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getAllArtists(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()
        assertThat(runResult1.getOrThrow().artists).hasSize(3)

        // forced refresh will make an api call
        val runResult2 = runCatching { stationRepository.getAllArtists(1, true) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()
        assertThat(runResult2.getOrThrow().artists).hasSize(3)
    }

    @Test
    fun getAllCategories_populates_cache_and_is_reused_on_subsequent_calls() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/all_groups" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/all-groups.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getAllCategories(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()

        // cache should be reused from here
        val runResult2 = runCatching { stationRepository.getAllCategories(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()

        assertThat(runResult1.getOrThrow()).isEqualTo(runResult2.getOrThrow())
        assertThat(runResult1.getOrThrow().categories).hasSize(2)
    }

    @Test
    fun getAllCategories_populates_cache_and_can_be_forced_refresh() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/all_groups" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/all-groups.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getAllCategories(1, false) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()
        assertThat(runResult1.getOrThrow().categories).hasSize(2)

        // forced refresh will make an api call
        val runResult2 = runCatching { stationRepository.getAllCategories(1, true) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()
        assertThat(runResult2.getOrThrow().categories).hasSize(2)
    }

    @Test
    fun getAlbum_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/album" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/album.json"))
                    .setResponseCode(HTTP_OK)
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getAlbum(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val album = runResult.getOrThrow().album
        assertThat(album.id).isEqualTo(3763)
        assertThat(album.name).isEqualTo(".hack//G.U.")
        assertThat(album.art).isEqualTo("/album_art/1_3763_320.jpg")
    }

    @Test
    fun getArtist_with_api_error() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/artist" -> MockResponse()
                    .setResponseCode(HTTP_NOT_FOUND)
                "/stations" -> MockResponse()
                    .setResponseCode(HTTP_OK)
                    .setBody(readFile(this.javaClass, "/json/stations.json"))
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getArtist(1, 1) }

        assertThat(runResult.isFailure).isTrue()
        assertThat(runResult.isSuccess).isFalse()
        assertThat(runResult.exceptionOrNull()!!).isInstanceOf(HttpException::class)
    }

    @Test
    fun getArtist_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/artist" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/artist.json"))
                    .setResponseCode(HTTP_OK)
                "/stations" -> MockResponse()
                    .setResponseCode(HTTP_OK)
                    .setBody(readFile(this.javaClass, "/json/stations.json"))
                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getArtist(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val stations = runResult.getOrThrow().first
        var station = stations[1]!!
        assertThat(station.id).isEqualTo(1)
        assertThat(station.name).isEqualTo("Game")

        station = stations[2]!!
        assertThat(station.id).isEqualTo(2)
        assertThat(station.name).isEqualTo("OC ReMix")

        station = stations[3]!!
        assertThat(station.id).isEqualTo(3)
        assertThat(station.name).isEqualTo("Covers")

        station = stations[4]!!
        assertThat(station.id).isEqualTo(4)
        assertThat(station.name).isEqualTo("Chiptune")

        station = stations[5]!!
        assertThat(station.id).isEqualTo(5)
        assertThat(station.name).isEqualTo("All")

        val artist = runResult.getOrThrow().second
        assertThat(artist.id).isEqualTo(9356)
        assertThat(artist.name).isEqualTo("Daisuke Ishiwatari")
        assertThat(artist.groupedSongs).isNotNull().isNotEmpty()
    }

    @Test
    fun getCategory_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/group" -> MockResponse()
                    .setBody(readFile(this.javaClass, "/json/category.json"))
                    .setResponseCode(HTTP_OK)

                else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getCategory(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val category = runResult.getOrThrow().category
        assertThat(category.id).isEqualTo(3752)
        assertThat(category.name).isEqualTo("Final Fantasy Tactics")

        val songs = category.songs
        assertThat(songs).isNotNull().isNotEmpty()
    }
}

package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.size
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.LAST_PLAYED
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.readFile
import io.mockk.every
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

class StationRepositoryStationListTest : BaseTest() {
    @Test
    fun getStations_returns_station_list() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getStations() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val stations = runResult.getOrThrow()
        assertThat(stations).size().isEqualTo(5)

        var station = stations[0]
        assertThat(station.id).isEqualTo(1)
        assertThat(station.name).isEqualTo("Game")
        assertThat(station.description).isEqualTo("Video game original soundtrack radio.  Vote for the song you want to hear!")
        assertThat(station.stream).isEqualTo("http://allrelays.rainwave.cc/game.mp3?12345:abcdef")
        assertThat(station.relays).isEqualTo(listOf(
            "https://relay.rainwave.cc/game.mp3?12345:abcdef",
        ))

        station = stations[1]
        assertThat(station.id).isEqualTo(2)
        assertThat(station.name).isEqualTo("OC ReMix")
        assertThat(station.description).isEqualTo("OverClocked ReMix Radio, the best in video game remixes.  Vote for your favourite remixes!")
        assertThat(station.stream).isEqualTo("http://allrelays.rainwave.cc/ocremix.mp3?12345:abcdef")
        assertThat(station.relays).isEqualTo(listOf(
            "https://relay.rainwave.cc/ocremix.mp3?12345:abcdef",
        ))

        station = stations[2]
        assertThat(station.id).isEqualTo(3)
        assertThat(station.name).isEqualTo("Covers")
        assertThat(station.description).isEqualTo("Video game official and fan-made remixes, streaming 24/7.  Vote for your favourite artists!")
        assertThat(station.stream).isEqualTo("http://allrelays.rainwave.cc/covers.mp3?12345:abcdef")
        assertThat(station.relays).isEqualTo(listOf(
            "http://allrelays.rainwave.cc/covers.mp3?12345:abcdef",
        ))

        station = stations[3]
        assertThat(station.id).isEqualTo(4)
        assertThat(station.name).isEqualTo("Chiptune")
        assertThat(station.description).isEqualTo("Video game and original chiptune soudtracks, streaming 24/7.  Vote for the songs you want to hear!")
        assertThat(station.stream).isEqualTo("http://allrelays.rainwave.cc/chiptune.mp3?12345:abcdef")
        assertThat(station.relays).isEqualTo(listOf(
            "http://relay.rainwave.cc:443/chiptune.mp3?12345:abcdef",
        ))

        station = stations[4]
        assertThat(station.id).isEqualTo(5)
        assertThat(station.name).isEqualTo("All")
        assertThat(station.description).isEqualTo("Video game music online radio, including remixes and original soundtracks!  Vote for the songs you want to hear!")
        assertThat(station.stream).isEqualTo("http://allrelays.rainwave.cc/all.mp3?12345:abcdef")
        assertThat(station.relays).isEqualTo(listOf(
            "http://relay.rainwave.cc/all.mp3?12345:abcdef",
        ))
    }

    @Test
    fun getStations_returns_station_list_when_retried() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_BAD_REQUEST))

            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        var runResult = runCatching { stationRepository.getStations() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isTrue()
        assertThat(runResult.isSuccess).isFalse()
        assertThat(runResult.exceptionOrNull()!!).isInstanceOf(HttpException::class)

        // retry call
        runResult = runCatching { stationRepository.getStations() }

        assertThat(mockWebServer.requestCount).isEqualTo(2)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val stations = runResult.getOrThrow()
        assertThat(stations).size().isEqualTo(5)
    }

    @Test
    fun getStations_returns_cached_stations_subsequently() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult1 = runCatching { stationRepository.getStations() }
        val runResult2 = runCatching { stationRepository.getStations() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult1.isFailure).isFalse()
        assertThat(runResult1.isSuccess).isTrue()
        assertThat(runResult2.isFailure).isFalse()
        assertThat(runResult2.isSuccess).isTrue()

        var stations = runResult1.getOrThrow()
        assertThat(stations).size().isEqualTo(5)

        stations = runResult2.getOrThrow()
        assertThat(stations).size().isEqualTo(5)
    }

    @Test
    fun getSuggestedStations_with_existing_last_played_returns_station_list_with_last_played_at_top_of_list() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        every { dataStoreMock.getBlocking(LAST_PLAYED) } returns 5

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getSuggestedStations() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val stations = runResult.getOrThrow()
        assertThat(stations).size().isEqualTo(5)

        assertThat(stations[0].id).isEqualTo(5)
        assertThat(stations[1].id).isEqualTo(1)
        assertThat(stations[2].id).isEqualTo(2)
        assertThat(stations[3].id).isEqualTo(3)
        assertThat(stations[4].id).isEqualTo(4)
    }

    @Test
    fun getSuggestedStations_without_existing_last_played_returns_station_list_without_modification() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        every { dataStoreMock.getBlocking(LAST_PLAYED) } returns -1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getSuggestedStations() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val stations = runResult.getOrThrow()
        assertThat(stations).size().isEqualTo(5)

        assertThat(stations[0].id).isEqualTo(1)
        assertThat(stations[1].id).isEqualTo(2)
        assertThat(stations[2].id).isEqualTo(3)
        assertThat(stations[3].id).isEqualTo(4)
        assertThat(stations[4].id).isEqualTo(5)
    }
}

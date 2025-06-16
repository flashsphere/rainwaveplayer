package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.LAST_PLAYED
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.readFile
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.net.HttpURLConnection.HTTP_OK

class StationRepositoryPreviousAndNextStationTest : BaseTest() {
    @Test
    fun getPrevStation_returns_prev_station_with_provided_current_station() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getPrevStation(Station(
            id = 5,
            name = "All",
            description = "Video game music online radio, including remixes and original soundtracks!  Vote for the songs you want to hear!",
            stream = "http://allrelays.rainwave.cc/all.mp3?12345:abcdef",
            relays = listOf("http://relay.rainwave.cc/all.mp3?12345:abcdef")))
        }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(4)
        assertThat(station.name).isEqualTo("Chiptune")
    }

    @Test
    fun getPrevStation_returns_prev_station_without_provided_current_station_but_with_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns 4

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getPrevStation() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(3)
        assertThat(station.name).isEqualTo("Covers")
    }

    @Test
    fun getPrevStation_returns_prev_station_without_provided_current_station_and_without_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns -1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getPrevStation() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(5)
        assertThat(station.name).isEqualTo("All")
    }

    @Test
    fun getNextStation_returns_next_station_with_provided_current_station() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getNextStation(Station(
            id = 3,
            name = "Covers",
            description = "Video game official and fan-made remixes, streaming 24/7.  Vote for your favourite artists!",
            stream = "http://allrelays.rainwave.cc/covers.mp3?12345:abcdef",
            relays = listOf("http://allrelays.rainwave.cc/covers.mp3?12345:abcdef")))
        }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(4)
        assertThat(station.name).isEqualTo("Chiptune")
    }

    @Test
    fun getNextStation_returns_next_station_without_provided_current_station_but_with_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns 5

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getNextStation() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(1)
        assertThat(station.name).isEqualTo("Game")
    }

    @Test
    fun getNextStation_returns_next_station_without_provided_current_station_and_without_existing_last_played() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)
        setupSavedStationsStoreMock()

        coEvery { dataStoreMock.get(LAST_PLAYED) } returns -1

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/stations.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.getNextStation() }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val station = runResult.getOrThrow()
        assertThat(station.id).isEqualTo(2)
        assertThat(station.name).isEqualTo("OC ReMix")
    }
}

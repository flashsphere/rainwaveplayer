package com.flashsphere.rainwaveplayer.model.station

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class StationAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/stations.json")
        val stationsResponse = json.decodeFromString<StationsResponse>(jsonString)

        assertThat(stationsResponse.stations.size).isEqualTo(5)

        var station = stationsResponse.stations[0]
        assertThat(station.id).isEqualTo(1)
        assertThat(station.name).isEqualTo("Game")
        assertThat(station.description).isEqualTo("Video game original soundtrack radio.  Vote for the song you want to hear!")
        assertThat(station.stream).isEqualTo("http://allrelays.rainwave.cc/game.mp3?12345:abcdef")
        assertThat(station.relays).isEqualTo(listOf(
            "https://relay.rainwave.cc/game.mp3?12345:abcdef",
        ))

        station = stationsResponse.stations[1]
        assertThat(station.relays).isEqualTo(listOf(
            "https://relay.rainwave.cc/ocremix.mp3?12345:abcdef",
        ))

        station = stationsResponse.stations[2]
        assertThat(station.relays).isEqualTo(listOf(
            "http://allrelays.rainwave.cc/covers.mp3?12345:abcdef",
        ))

        station = stationsResponse.stations[3]
        assertThat(station.relays).isEqualTo(listOf(
            "http://relay.rainwave.cc:443/chiptune.mp3?12345:abcdef",
        ))

        station = stationsResponse.stations[4]
        assertThat(station.relays).isEqualTo(listOf(
            "http://relay.rainwave.cc/all.mp3?12345:abcdef",
        ))
    }

    @Test
    fun stations_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/stations-error.json")
        val result = json.decodeFromString<StationsErrorResponse>(jsonString).result

        assertThat(result.code).isEqualTo(403)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Authorization failed.")
    }
}

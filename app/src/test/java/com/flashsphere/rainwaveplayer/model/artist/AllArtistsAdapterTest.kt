package com.flashsphere.rainwaveplayer.model.artist

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class AllArtistsAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/all-artists.json")
        val allArtists = json.decodeFromString<AllArtists>(jsonString)

        assertThat(allArtists).isNotNull()
        assertThat(allArtists.artists.size).isEqualTo(3)

        var artist = allArtists.artists[0]
        assertThat(artist.id).isEqualTo(22962)
        assertThat(artist.name).isEqualTo("19's Sound Factory")

        artist = allArtists.artists[2]
        assertThat(artist.id).isEqualTo(25092)
        assertThat(artist.name).isEqualTo("Kristofer Maddigan")
    }
}

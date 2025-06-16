package com.flashsphere.rainwaveplayer.model.search

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class SearchAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun search_response_works() {
        val jsonString = readFile(this.javaClass, "/json/search.json")
        val searchResponse = json.decodeFromString<SearchResponse>(jsonString)

        assertThat(searchResponse.result).isNull()
        assertThat(searchResponse.artists.size).isEqualTo(1)
        assertThat(searchResponse.albums.size).isEqualTo(1)
        assertThat(searchResponse.songs.size).isEqualTo(2)

        val artist = searchResponse.artists.first()
        assertThat(artist.id).isEqualTo(9015)
        assertThat(artist.name).isEqualTo("nervous_testpilot")

        val album = searchResponse.albums.first()
        assertThat(album.id).isEqualTo(379)
        assertThat(album.name).isEqualTo("Fittest")
        assertThat(album.cool).isEqualTo(false)
        assertThat(album.favorite).isEqualTo(false)
        assertThat(album.rating).isEqualTo(4.2F)
        assertThat(album.ratingUser).isEqualTo(0F)

        val song = searchResponse.songs.first()
        assertThat(song.id).isEqualTo(100)
        assertThat(song.title).isEqualTo("Testing Times")
        assertThat(song.rating).isEqualTo(3.7F)
        assertThat(song.ratingUser).isEqualTo(0F)
        assertThat(song.getAlbumName()).isEqualTo("Banjo-Kazooie: Nuts & Bolts")
        assertThat(song.ratingAllowed).isEqualTo(false)
        assertThat(song.entryId).isEqualTo(0)
        assertThat(song.cool).isEqualTo(false)
        assertThat(song.requestable).isEqualTo(true)
        assertThat(song.favorite).isEqualTo(false)
        assertThat(song.userIdRequested).isEqualTo(0)
        assertThat(song.usernameRequested).isEqualTo("")
        assertThat(song.stationId).isEqualTo(1)
        assertThat(song.voted).isEqualTo(false)
    }

    @Test
    fun search_error_response_work() {
        val jsonString = readFile(this.javaClass, "/json/search-error.json")
        val searchResponse = json.decodeFromString<SearchResponse>(jsonString)

        assertThat(searchResponse.result?.code).isEqualTo(403)
        assertThat(searchResponse.result?.success).isEqualTo(false)
        assertThat(searchResponse.result?.text).isEqualTo("Authorization failed.")
        assertThat(searchResponse.artists).isEmpty()
        assertThat(searchResponse.albums).isEmpty()
        assertThat(searchResponse.songs).isEmpty()
    }
}

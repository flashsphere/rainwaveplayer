package com.flashsphere.rainwaveplayer.model.favorite

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class FavoriteAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun fave_song_response_works() {
        val jsonString = readFile(this.javaClass, "/json/fave-song.json")
        val result = json.decodeFromString<FaveSongResponse>(jsonString).result

        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Favourited song.")
        assertThat(result.favorite).isEqualTo(true)
        assertThat(result.id).isEqualTo(25475)
        assertThat(result.stationId).isEqualTo(1)
    }

    @Test
    fun fave_song_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/fave-song-error.json")
        val result = json.decodeFromString<FaveSongResponse>(jsonString).result

        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid argument 'song_id': song_id must be a valid song ID.")
        assertThat(result.favorite).isEqualTo(false)
        assertThat(result.id).isEqualTo(0)
        assertThat(result.stationId).isEqualTo(0)
    }

    @Test
    fun fave_album_response_works() {
        val jsonString = readFile(this.javaClass, "/json/fave-album.json")
        val result = json.decodeFromString<FaveAlbumResponse>(jsonString).result

        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Favourited album.")
        assertThat(result.favorite).isEqualTo(true)
        assertThat(result.id).isEqualTo(3763)
        assertThat(result.stationId).isEqualTo(1)
    }

    @Test
    fun fave_album_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/fave-album-error.json")
        val result = json.decodeFromString<FaveAlbumResponse>(jsonString).result

        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid argument 'album_id': album_id must be a valid album ID.")
        assertThat(result.favorite).isEqualTo(false)
        assertThat(result.id).isEqualTo(0)
        assertThat(result.stationId).isEqualTo(0)
    }
}

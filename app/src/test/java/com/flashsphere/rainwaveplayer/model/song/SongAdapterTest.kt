package com.flashsphere.rainwaveplayer.model.song

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class SongAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/song.json")
        val song = json.decodeFromString<Song>(jsonString)

        assertThat(song.id).isEqualTo(1073)
        assertThat(song.title).isEqualTo("Ada's Theme")
        assertThat(song.rating).isEqualTo(3.7F)
        assertThat(song.ratingUser).isEqualTo(0F)
        assertThat(song.getArtistName()).isEqualTo("Masami Ueda, Shusaku Uchiyama, Shun Nishigaki")
        assertThat(song.getAlbumName()).isEqualTo("Resident Evil 2")
        assertThat(song.getAlbumCoverUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_116_320.jpg")
        assertThat(song.ratingAllowed).isEqualTo(false)
        assertThat(song.entryId).isEqualTo(16399608)
        assertThat(song.cool).isEqualTo(false)
        assertThat(song.requestable).isEqualTo(false)
        assertThat(song.favorite).isEqualTo(true)
        assertThat(song.userIdRequested).isEqualTo(12345)
        assertThat(song.usernameRequested).isEqualTo("John Doe")
        assertThat(song.stationId).isEqualTo(1)
        assertThat(song.voted).isEqualTo(false)
    }

    @Test
    fun adapter_works_with_separate_album_names() {
        val jsonString = """{
            "title": "Ada's Theme",
            "id": 1073,
            "album_name": "Resident Evil 2",
            "album_id": 116
          }""".trimIndent()

        val song = json.decodeFromString<Song>(jsonString)
        assertThat(song.id).isEqualTo(1073)
        assertThat(song.title).isEqualTo("Ada's Theme")

        val album = song.albums.first()
        assertThat(album.id).isEqualTo(116)
        assertThat(album.name).isEqualTo("Resident Evil 2")
    }

    @Test
    fun rate_song_response_works() {
        val jsonString = readFile(this.javaClass, "/json/rate-song.json")
        val response = json.decodeFromString<RateSongResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Rating submitted.")
        assertThat(result.rating).isEqualTo(3.5F)
        assertThat(result.songId).isEqualTo(3470)
    }

    @Test
    fun rate_song_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/rate-song-error.json")
        val response = json.decodeFromString<RateSongResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(200)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Cannot rate that song at this time.")
        assertThat(result.rating).isEqualTo(0F)
        assertThat(result.songId).isEqualTo(0)
    }

    @Test
    fun clear_rating_response_works() {
        val jsonString = readFile(this.javaClass, "/json/clear-rating.json")
        val response = json.decodeFromString<RateSongResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Rating submitted.")
        assertThat(result.rating).isEqualTo(0F)
        assertThat(result.songId).isEqualTo(4249)
    }

    @Test
    fun clear_rating_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/clear-rating-error.json")
        val response = json.decodeFromString<RateSongResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid argument 'song_id': song_id must be a valid song ID.")
        assertThat(result.rating).isEqualTo(0F)
        assertThat(result.songId).isEqualTo(0)
    }
}

package com.flashsphere.rainwaveplayer.model.album

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class AlbumAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/album.json")
        val albumResponse = json.decodeFromString<AlbumResponse>(jsonString)
        val album = albumResponse.album

        assertThat(album.id).isEqualTo(3763)
        assertThat(album.name).isEqualTo(".hack//G.U.")
        assertThat(album.art).isEqualTo("/album_art/1_3763_320.jpg")
        assertThat(album.getArtUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_3763_320.jpg")
        assertThat(album.rating).isEqualTo(3.9F)
        assertThat(album.ratingUser).isEqualTo(1.5F)
        assertThat(album.cool).isEqualTo(true)
        assertThat(album.ratingCount).isEqualTo(533)
        assertThat(album.favorite).isEqualTo(true)
        assertThat(album.ratingDistribution).isEqualTo(mapOf(
            1 to 0.05F,
            2 to 0.23F,
            3 to 1F,
            4 to 0.88F,
            5 to 0.22F))
    }
}

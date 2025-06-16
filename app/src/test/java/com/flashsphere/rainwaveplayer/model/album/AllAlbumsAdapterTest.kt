package com.flashsphere.rainwaveplayer.model.album

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class AllAlbumsAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/all-albums.json")
        val allAlbums = json.decodeFromString<AllAlbums>(jsonString)

        assertThat(allAlbums).isNotNull()
        assertThat(allAlbums.albums.size).isEqualTo(3)

        var album = allAlbums.albums[0]
        assertThat(album.id).isEqualTo(3370)
        assertThat(album.name).isEqualTo("100% Orange Juice")
        assertThat(album.rating).isEqualTo(4.0F)
        assertThat(album.ratingUser).isEqualTo(0.0F)
        assertThat(album.cool).isEqualTo(true)
        assertThat(album.favorite).isEqualTo(false)
        assertThat(album).isEqualTo(allAlbums.albumMap[album.id])

        album = allAlbums.albums[2]
        assertThat(album.id).isEqualTo(424)
        assertThat(album.name).isEqualTo("Bayonetta")
        assertThat(album.rating).isEqualTo(4.0F)
        assertThat(album.ratingUser).isEqualTo(1.5F)
        assertThat(album.cool).isEqualTo(false)
        assertThat(album.favorite).isEqualTo(true)
        assertThat(album).isEqualTo(allAlbums.albumMap[album.id])
    }
}

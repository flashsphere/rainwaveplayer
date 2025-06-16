package com.flashsphere.rainwaveplayer.model.artist

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class ArtistAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/artist.json")
        val artistResponse = json.decodeFromString<ArtistResponse>(jsonString)
        val artist = artistResponse.artist

        assertThat(artist.id).isEqualTo(9356)
        assertThat(artist.name).isEqualTo("Daisuke Ishiwatari")
        assertThat(artist.groupedSongs).isNotNull().isNotEmpty()

        val groupedSongs = artist.groupedSongs
        assertThat(groupedSongs[1]).isNotNull().isNotEmpty()
        assertThat(groupedSongs[2]).isNull()
        assertThat(groupedSongs[3]).isNull()
        assertThat(groupedSongs[4]).isNotNull().isNotEmpty()
        assertThat(groupedSongs[5]).isNull()

        // station id: 1
        val albumSongsForStation1 = groupedSongs[1]!!
        assertThat(albumSongsForStation1.size).isEqualTo(2)
        val albumsForStation1 = ArrayList(albumSongsForStation1.keys)
        assertThat(albumsForStation1[0].id).isEqualTo(354)
        assertThat(albumsForStation1[0].name).isEqualTo("BlazBlue: Chronophantasma")
        assertThat(albumSongsForStation1[albumsForStation1[0]]?.size).isEqualTo(2)
        assertThat(albumsForStation1[1].id).isEqualTo(3508)
        assertThat(albumsForStation1[1].name).isEqualTo("Guilty Gear Xrd")
        assertThat(albumSongsForStation1[albumsForStation1[1]]?.size).isEqualTo(1)

        // station id: 4
        val albumSongsForStation4 = groupedSongs[4]!!
        assertThat(albumSongsForStation4.size).isEqualTo(1)
        val albumsForStation4 = ArrayList(albumSongsForStation4.keys)
        assertThat(albumsForStation4[0].id).isEqualTo(2139)
        assertThat(albumsForStation4[0].name).isEqualTo("Guilty Gear Petit")
        assertThat(albumSongsForStation4[albumsForStation4[0]]?.size).isEqualTo(1)
    }
}

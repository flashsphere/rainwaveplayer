package com.flashsphere.rainwaveplayer.model.category

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class CategoryAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/category.json")
        val categoryResponse = json.decodeFromString<CategoryResponse>(jsonString)
        val category = categoryResponse.category

        assertThat(category.id).isEqualTo(3752)
        assertThat(category.name).isEqualTo("Final Fantasy Tactics")

        val songs = category.songs
        assertThat(songs).isNotNull().isNotEmpty()

        val albums = ArrayList(songs.keys)
        assertThat(albums[0].id).isEqualTo(323)
        assertThat(albums[0].name).isEqualTo("Final Fantasy Tactics")

        val albumSongs = songs[albums[1]]!!
        assertThat(albumSongs.size).isEqualTo(2)
        assertThat(albumSongs[0].id).isEqualTo(2891)
        assertThat(albumSongs[0].title).isEqualTo("Before And Behind")
    }
}

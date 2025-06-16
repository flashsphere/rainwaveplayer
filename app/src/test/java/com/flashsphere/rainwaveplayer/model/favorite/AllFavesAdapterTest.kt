package com.flashsphere.rainwaveplayer.model.favorite

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class AllFavesAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun all_faves_response_works() {
        val jsonString = readFile(this.javaClass, "/json/all-faves.json")
        val result = json.decodeFromString<AllFavesResponse>(jsonString).songs

        assertAll {
            assertThat(result).hasSize(10)

            with(result[0]) {
                assertThat(this.id).isEqualTo(1861)
                assertThat(this.title).isEqualTo("Megalith-Agnus Dei")
                assertThat(this.albums[0].id).isEqualTo(205)
                assertThat(this.albums[0].name).isEqualTo("Ace Combat 04")
                assertThat(this.rating).isEqualTo(4.4F)
                assertThat(this.ratingUser).isEqualTo(4.5F)
                assertThat(this.favorite).isTrue()
            }
        }
    }
}

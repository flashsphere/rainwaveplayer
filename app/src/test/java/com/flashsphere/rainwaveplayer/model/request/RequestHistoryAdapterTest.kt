package com.flashsphere.rainwaveplayer.model.request

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class RequestHistoryAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun recent_votes_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-history.json")
        val result = json.decodeFromString<RequestHistoryResponse>(jsonString).songs

        assertAll {
            assertThat(result).hasSize(10)

            with(result[0]) {
                assertThat(this.id).isEqualTo(2187)
                assertThat(this.title).isEqualTo("Venus Lighthouse")
                assertThat(this.albums[0].id).isEqualTo(0)
                assertThat(this.albums[0].name).isEqualTo("Golden Sun")
                assertThat(this.rating).isEqualTo(4.7F)
                assertThat(this.ratingUser).isEqualTo(0.0F)
                assertThat(this.favorite).isTrue()
            }
            with(result[1]) {
                assertThat(this.id).isEqualTo(33768)
                assertThat(this.title).isEqualTo("Theme from Ni no Kuni II")
                assertThat(this.albums[0].id).isEqualTo(0)
                assertThat(this.albums[0].name).isEqualTo("Ni no Kuni II: Revenant Kingdom")
                assertThat(this.rating).isEqualTo(4.7F)
                assertThat(this.ratingUser).isEqualTo(5.0F)
                assertThat(this.favorite).isFalse()
            }
        }
    }
}

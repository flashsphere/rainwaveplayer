package com.flashsphere.rainwaveplayer.model.vote

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

class RecentVotesAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun recent_votes_response_works() {
        val jsonString = readFile(this.javaClass, "/json/recent-votes.json")
        val result = json.decodeFromString<RecentVotesResponse>(jsonString).songs

        assertAll {
            assertThat(result).hasSize(10)

            with(result[0]) {
                assertThat(this.id).isEqualTo(2085)
                assertThat(this.title).isEqualTo("The Street of Rage")
                assertThat(this.albums[0].id).isEqualTo(0)
                assertThat(this.albums[0].name).isEqualTo("Streets of Rage")
                assertThat(this.rating).isEqualTo(4.5F)
                assertThat(this.ratingUser).isEqualTo(4.0F)
                assertThat(this.favorite).isTrue()
            }
            with(result[1]) {
                assertThat(this.id).isEqualTo(27097)
                assertThat(this.title).isEqualTo("Lorule Theme")
                assertThat(this.albums[0].id).isEqualTo(0)
                assertThat(this.albums[0].name).isEqualTo("The Legend of Zelda: A Link Between Worlds")
                assertThat(this.rating).isEqualTo(5.0F)
                assertThat(this.ratingUser).isEqualTo(0.0F)
                assertThat(this.favorite).isFalse()
            }
        }
    }
}

package com.flashsphere.rainwaveplayer.model.vote

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class VoteAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun vote_song_response_works() {
        val jsonString = readFile(this.javaClass, "/json/vote.json")
        val result = json.decodeFromString<VoteResponse>(jsonString).result

        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Vote Submitted")
    }

    @Test
    fun vote_song_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/vote-error.json")
        val result = json.decodeFromString<VoteResponse>(jsonString).result

        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Cannot vote for that song right now.")
    }
}

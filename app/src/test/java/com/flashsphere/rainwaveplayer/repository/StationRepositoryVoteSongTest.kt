package com.flashsphere.rainwaveplayer.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.BaseTest
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.net.HttpURLConnection.HTTP_OK

class StationRepositoryVoteSongTest : BaseTest() {
    @Test
    fun vote_song_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/vote.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.vote(1, 1) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Vote Submitted")
    }
}

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

class StationRepositoryFavoriteTest : BaseTest() {
    @Test
    fun fave_song_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/fave-song.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.favoriteSong(1, true) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Favourited song.")
    }

    @Test
    fun fave_album_returns_result() = runTest(testDispatcher) {
        setupAppCoroutineDispatchers(backgroundScope)

        mockWebServer.apply {
            enqueue(MockResponse()
                .setResponseCode(HTTP_OK)
                .setBody(readFile(this.javaClass, "/json/fave-album.json")))
        }

        val stationRepository = createStationRepository()
        val runResult = runCatching { stationRepository.favoriteAlbum(1, 1, true) }

        assertThat(mockWebServer.requestCount).isEqualTo(1)

        assertThat(runResult.isFailure).isFalse()
        assertThat(runResult.isSuccess).isTrue()

        val result = runResult.getOrThrow().result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Favourited album.")
    }
}

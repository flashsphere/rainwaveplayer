package com.flashsphere.rainwaveplayer.model.request

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.size
import com.flashsphere.rainwaveplayer.model.user.User
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class RequestAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    // request_song
    @Test
    fun request_song_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-song.json")
        val response = json.decodeFromString<RequestSongResponse>(jsonString)

        assertThat(response.requests).size().isEqualTo(1)
        assertRequest(response.requests.first())
    }

    @Test
    fun request_song_unsuccessful_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-song-unsuccessful.json")
        val result = json.decodeFromString<RequestSongResponse>(jsonString).result

        assertThat(result.code).isEqualTo(200)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Album already requested.")
    }

    @Test
    fun request_song_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-song-error.json")
        val result = json.decodeFromString<RequestSongResponse>(jsonString).result

        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid argument 'song_id': song_id must be a valid song ID that exists on the requested station ID.")
    }

    // clear requests
    @Test
    fun clear_request_response_works() {
        val jsonString = readFile(this.javaClass, "/json/clear-requests.json")
        val response = json.decodeFromString<ClearRequestsResponse>(jsonString)

        assertThat(response.requests).isEmpty()
    }

    @Test
    fun clear_request_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/clear-requests-error.json")
        val result = json.decodeFromString<ClearRequestsErrorResponse>(jsonString).result

        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid station ID.")
    }

    // delete request
    @Test
    fun delete_request_response_works() {
        val jsonString = readFile(this.javaClass, "/json/delete-request.json")
        val response = json.decodeFromString<DeleteRequestResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ request_deleted ]]")
        assertThat(response.requests).size().isEqualTo(1)
        assertRequest(response.requests.first())
    }

    @Test
    fun delete_request_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/delete-request-error.json")
        val response = json.decodeFromString<DeleteRequestResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(200)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Song not requested.")
        assertThat(response.requests).isEmpty()
    }

    // order requests
    @Test
    fun order_requests_response_works() {
        val jsonString = readFile(this.javaClass, "/json/order-requests.json")
        val response = json.decodeFromString<OrderRequestsResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("Requests reordered.")
        assertThat(response.requests).size().isEqualTo(2)
        assertRequest(response.requests.first())
    }

    @Test
    fun order_requests_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/order-requests-error.json")
        val response = json.decodeFromString<OrderRequestsResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid argument 'order': order must be a comma-separated list of valid song IDs.")
        assertThat(response.requests).isEmpty()
    }

    // pause requests
    @Test
    fun pause_requests_response_works() {
        val jsonString = readFile(this.javaClass, "/json/pause-requests.json")
        val response = json.decodeFromString<PauseRequestResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ radio_requests_paused ]]")
        assertUser(response.user!!)
    }

    @Test
    fun pause_requests_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/pause-requests-error.json")
        val response = json.decodeFromString<PauseRequestResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid station ID.")
        assertThat(response.user).isNull()
    }

    // resume requests
    @Test
    fun resume_requests_response_works() {
        val jsonString = readFile(this.javaClass, "/json/resume-requests.json")
        val response = json.decodeFromString<ResumeRequestResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ radio_requests_unpaused ]]")
        assertUser(response.user!!)
    }

    @Test
    fun resume_requests_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/resume-requests-error.json")
        val response = json.decodeFromString<ResumeRequestResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(400)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Invalid station ID.")
        assertThat(response.user).isNull()
    }

    // request fave
    @Test
    fun request_fave_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-fave.json")
        val response = json.decodeFromString<RequestFaveResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ request_favorited_songs_success ]]")
        assertThat(response.requests).size().isEqualTo(1)
        assertRequest(response.requests.first())
    }

    @Test
    fun request_fave_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-fave-error.json")
        val response = json.decodeFromString<RequestFaveResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(200)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Your queue is full.")
        assertThat(response.requests).isEmpty()
    }

    // request unrated
    @Test
    fun request_unrated_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-unrated.json")
        val response = json.decodeFromString<RequestUnratedResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(0)
        assertThat(result.success).isEqualTo(true)
        assertThat(result.text).isEqualTo("[[ request_unrated_songs_success ]]")
        assertThat(response.requests).size().isEqualTo(1)
        assertRequest(response.requests.first())
    }

    @Test
    fun request_unrated_error_response_works() {
        val jsonString = readFile(this.javaClass, "/json/request-unrated-error.json")
        val response = json.decodeFromString<RequestUnratedResponse>(jsonString)

        val result = response.result
        assertThat(result.code).isEqualTo(200)
        assertThat(result.success).isEqualTo(false)
        assertThat(result.text).isEqualTo("Your queue is full.")
        assertThat(response.requests).isEmpty()
    }

    private fun assertRequest(request: Request) {
        assertThat(request.requestId).isEqualTo(8034242)
        assertThat(request.songId).isEqualTo(30748)
        assertThat(request.stationId).isEqualTo(1)
        assertThat(request.title).isEqualTo("Iclucian Dance")
        assertThat(request.valid).isEqualTo(false)
        assertThat(request.good).isEqualTo(true)
        assertThat(request.getAlbumName()).isEqualTo("Ys VIII: Lacrimosa of Dana")
        assertThat(request.getAlbumCoverUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_3503_320.jpg")
        assertThat(request.rating).isEqualTo(4.6F)
        assertThat(request.ratingUser).isEqualTo(5.0F)
        assertThat(request.favorite).isEqualTo(true)
        assertThat(request.cool).isEqualTo(false)
        assertThat(request.coolEndTime).isEqualTo(1602008252)
        assertThat(request.electionBlocked).isEqualTo(true)
        assertThat(request.electionBlockedBy).isEqualTo("group")
    }

    private fun assertUser(user: User) {
        assertThat(user.id).isEqualTo(222)
        assertThat(user.name).isEqualTo("pramoda")
        assertThat(user.avatar).isEqualTo("/static/images4/user.svg")
        assertThat(user.getAvatarUrl()).isEqualTo("${RainwaveService.BASE_URL}/static/images4/user.svg")
        assertThat(user.requestsPaused).isEqualTo(false)
        assertThat(user.requestPosition).isEqualTo(3)
    }
}

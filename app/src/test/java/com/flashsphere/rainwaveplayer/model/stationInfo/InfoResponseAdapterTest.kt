package com.flashsphere.rainwaveplayer.model.stationInfo

import androidx.collection.mutableScatterMapOf
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.flashsphere.rainwaveplayer.model.event.Event
import com.flashsphere.rainwaveplayer.model.request.Request
import com.flashsphere.rainwaveplayer.model.requestLine.RequestLine
import com.flashsphere.rainwaveplayer.model.user.User
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class InfoResponseAdapterTest {
    private lateinit var json: Json

    @Before
    fun setup() {
        json = buildJson()
    }

    @Test
    fun adapter_works() {
        val jsonString = readFile(this.javaClass, "/json/info-response.json")
        val infoResponse = json.decodeFromString<InfoResponse>(jsonString)

        assertThat(infoResponse).isNotNull()

        val apiInfo = infoResponse.apiInfo
        assertThat(apiInfo.time).isEqualTo(1601898553)

        assertRequestLine(infoResponse.requestLine)
        assertAlreadyVoted(infoResponse)
        assertPastEvents(infoResponse.previousEvents)
        assertCurrentEvent(infoResponse.currentEvent)
        assertFutureEvents(infoResponse.futureEvents)
        assertRequests(infoResponse.requests)
        assertUser(infoResponse.user)
    }

    private fun assertRequestLine(requestLine: List<RequestLine>) {
        var request = requestLine.first()
        assertThat(request.username).isEqualTo("Laharl")
        assertThat(request.song).isNull()
        assertThat(request.position).isEqualTo(1)

        request = requestLine.last()
        assertThat(request.username).isEqualTo("Squirtle129")
        assertThat(request.position).isEqualTo(4)

        val requestLineSong = request.song!!
        assertThat(requestLineSong.id).isEqualTo(2085)
        assertThat(requestLineSong.title).isEqualTo("The Street of Rage")
        assertThat(requestLineSong.albumName).isEqualTo("Streets of Rage")
    }

    private fun assertAlreadyVoted(infoResponse: InfoResponse) {
        assertThat(infoResponse.alreadyVoted).isEqualTo(mutableScatterMapOf(5568664 to 16397283))
    }

    private fun assertPastEvents(previousEvents: List<Event>) {
        assertThat(previousEvents.size).isEqualTo(5)

        val event = previousEvents.first()

        assertThat(event.id).isEqualTo(5569413)
        assertThat(event.name).isEqualTo("")
        assertThat(event.type).isEqualTo("Election")
        assertThat(event.stationId).isEqualTo(1)
        assertThat(event.startTime).isEqualTo(1601897853)
        assertThat(event.endTime).isEqualTo(1601898158)
        assertThat(event.votingAllowed).isEqualTo(false)
        assertThat(event.songs.size).isEqualTo(1)

        val song = event.songs.first()
        assertThat(song.id).isEqualTo(1633)
        assertThat(song.title).isEqualTo("Innocent Primeval Breaker")
        assertThat(song.rating).isEqualTo(4.7F)
        assertThat(song.ratingUser).isEqualTo(5.0F)
        assertThat(song.getArtistName()).isEqualTo("Falcom Sound Team jdk")
        assertThat(song.getAlbumName()).isEqualTo("Ys Seven")
        assertThat(song.getAlbumCoverUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_183_320.jpg")
        assertThat(song.ratingAllowed).isEqualTo(true)
        assertThat(song.entryId).isEqualTo(16399529)
        assertThat(song.cool).isEqualTo(true)
        assertThat(song.requestable).isEqualTo(false)
        assertThat(song.favorite).isEqualTo(true)
        assertThat(song.userIdRequested).isEqualTo(15182)
        assertThat(song.usernameRequested).isEqualTo("Laharl")
        assertThat(song.stationId).isEqualTo(1)
        assertThat(song.voted).isEqualTo(false)
        assertThat(song.votingAllowed).isEqualTo(false)
    }

    private fun assertCurrentEvent(event: Event) {
        assertThat(event.id).isEqualTo(5569439)
        assertThat(event.name).isEqualTo("")
        assertThat(event.type).isEqualTo("Election")
        assertThat(event.stationId).isEqualTo(1)
        assertThat(event.startTime).isEqualTo(1601898525)
        assertThat(event.endTime).isEqualTo(1601898607)
        assertThat(event.votingAllowed).isEqualTo(false)
        assertThat(event.songs.size).isEqualTo(3)

        val song = event.songs.first()
        assertThat(song.id).isEqualTo(30014)
        assertThat(song.title).isEqualTo("Roll'n Roll'n")
        assertThat(song.rating).isEqualTo(4.3F)
        assertThat(song.ratingUser).isEqualTo(0F)
        assertThat(song.getArtistName()).isEqualTo("Chikayo Fukuda")
        assertThat(song.getAlbumName()).isEqualTo("Solatorobo")
        assertThat(song.getAlbumCoverUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_3431_320.jpg")
        assertThat(song.ratingAllowed).isEqualTo(true)
        assertThat(song.entryId).isEqualTo(16399607)
        assertThat(song.cool).isEqualTo(false)
        assertThat(song.requestable).isEqualTo(false)
        assertThat(song.favorite).isEqualTo(false)
        assertThat(song.userIdRequested).isEqualTo(0)
        assertThat(song.usernameRequested).isEqualTo("")
        assertThat(song.stationId).isEqualTo(1)
        assertThat(song.voted).isEqualTo(false)
        assertThat(song.votingAllowed).isEqualTo(false)
    }

    private fun assertFutureEvents(futureEvents: List<Event>) {
        assertThat(futureEvents.size).isEqualTo(2)

        val event = futureEvents.first()

        assertThat(event.id).isEqualTo(5569440)
        assertThat(event.name).isEqualTo("")
        assertThat(event.type).isEqualTo("Election")
        assertThat(event.stationId).isEqualTo(1)
        assertThat(event.startTime).isEqualTo(0)
        assertThat(event.endTime).isEqualTo(1602031825)
        assertThat(event.votingAllowed).isEqualTo(true)
        assertThat(event.songs.size).isEqualTo(3)

        val song = event.songs.first()
        assertThat(song.id).isEqualTo(28847)
        assertThat(song.title).isEqualTo("Intro/Panic Puppet Zone - Act 1")
        assertThat(song.rating).isEqualTo(4.2F)
        assertThat(song.ratingUser).isEqualTo(0F)
        assertThat(song.getArtistName()).isEqualTo("Jun Senoue")
        assertThat(song.getAlbumName()).isEqualTo("Sonic 3D Blast (GEN)")
        assertThat(song.getAlbumCoverUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_2919_320.jpg")
        assertThat(song.ratingAllowed).isEqualTo(false)
        assertThat(song.entryId).isEqualTo(16399610)
        assertThat(song.cool).isEqualTo(false)
        assertThat(song.requestable).isEqualTo(false)
        assertThat(song.favorite).isEqualTo(false)
        assertThat(song.userIdRequested).isEqualTo(28173)
        assertThat(song.usernameRequested).isEqualTo("Squirtle129")
        assertThat(song.stationId).isEqualTo(1)
        assertThat(song.voted).isEqualTo(false)
        assertThat(song.votingAllowed).isEqualTo(true)
    }

    private fun assertRequests(requests: List<Request>) {
        assertThat(requests.size).isEqualTo(2)

        val request = requests.last()
        assertThat(request.requestId).isEqualTo(8016045)
        assertThat(request.songId).isEqualTo(12736)
        assertThat(request.stationId).isEqualTo(2)
        assertThat(request.title).isEqualTo("Made in U.S.A.")
        assertThat(request.valid).isEqualTo(true)
        assertThat(request.good).isEqualTo(true)
        assertThat(request.getAlbumName()).isEqualTo("Super Street Fighter II Turbo HD")
        assertThat(request.getAlbumCoverUrl()).isEqualTo("${RainwaveService.BASE_URL}/album_art/1_1557_320.jpg")
        assertThat(request.rating).isEqualTo(4.4F)
        assertThat(request.ratingUser).isEqualTo(4.0F)
        assertThat(request.favorite).isEqualTo(true)
        assertThat(request.cool).isEqualTo(false)
        assertThat(request.coolEndTime).isEqualTo(1601876795)
        assertThat(request.electionBlocked).isEqualTo(false)
        assertThat(request.electionBlockedBy).isEqualTo("group")
        assertThat(request.stationName).isEqualTo("")
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

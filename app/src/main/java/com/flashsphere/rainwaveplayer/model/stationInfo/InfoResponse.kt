package com.flashsphere.rainwaveplayer.model.stationInfo

import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.collection.mutableScatterMapOf
import com.flashsphere.rainwaveplayer.model.ApiInfo
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.event.Event
import com.flashsphere.rainwaveplayer.model.request.Request
import com.flashsphere.rainwaveplayer.model.requestLine.POSITION_COMPARATOR
import com.flashsphere.rainwaveplayer.model.requestLine.RequestLine
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.user.User
import com.flashsphere.rainwaveplayer.util.getTimeRemaining
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = InfoResponseSerializer::class)
class InfoResponse(
    val apiInfo: ApiInfo,
    val requestLine: List<RequestLine>,
    val alreadyVoted: ScatterMap<Int, Int>,
    val previousEvents: List<Event>,
    val currentEvent: Event,
    val futureEvents: List<Event>,
    var requests: List<Request>,
    var user: User,
) {
    val songsMap: ScatterMap<Int, Song> = mutableScatterMapOf<Int, Song>().apply {
        (previousEvents + currentEvent + futureEvents).asSequence()
            .flatMap { it.songs }
            .forEach { this.put(it.id, it) }
    }
    val albumsMap: ScatterMap<Int, Album> = mutableScatterMapOf<Int, Album>().apply {
        (previousEvents + currentEvent + futureEvents).asSequence()
            .flatMap { it.songs }
            .flatMap { it.albums }
            .forEach { this.put(it.id, it) }
    }

    fun isSongVoted(eventId: Int, entryId: Int): Boolean {
        return alreadyVoted[eventId] == entryId
    }

    fun getCurrentPosition(): Long {
        return currentEvent.songs.first().length - getTimeRemaining()
    }

    fun getCurrentSong() = currentEvent.songs.first()
}

@Serializable
private class InfoResponseSurrogate(
    val api_info: ApiInfo? = null,
    val request_line: List<RequestLine> = emptyList(),
    val already_voted: List<List<Int>> = emptyList(),
    val sched_history: List<Event> = emptyList(),
    val sched_current: Event? = null,
    val sched_next: List<Event> = emptyList(),
    val requests: List<Request> = emptyList(),
    val user: User? = null,
)

object InfoResponseSerializer : KSerializer<InfoResponse> {
    override val descriptor: SerialDescriptor = InfoResponseSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): InfoResponse {
        val surrogate = decoder.decodeSerializableValue(InfoResponseSurrogate.serializer())
        return InfoResponse(
            surrogate.api_info!!,
            surrogate.request_line.sortedWith(POSITION_COMPARATOR),
            getAlreadyVoted(surrogate.already_voted),
            getPastEvents(surrogate.sched_history),
            surrogate.sched_current!!,
            surrogate.sched_next,
            surrogate.requests,
            surrogate.user!!,
        )
    }

    override fun serialize(encoder: Encoder, value: InfoResponse) {
    }

    private fun getAlreadyVoted(alreadyVoted: List<List<Int>>?): ScatterMap<Int, Int> {
        if (alreadyVoted.isNullOrEmpty()) {
            return emptyScatterMap()
        }

        val votedList = alreadyVoted.last()
        if (votedList.isEmpty() || votedList.size != 2) {
            return emptyScatterMap()
        }

        return mutableScatterMapOf(votedList[0] to votedList[1])
    }

    private fun getPastEvents(events: List<Event>?): List<Event> {
        if (events.isNullOrEmpty()) return emptyList()

        for (event in events) {
            event.songs = listOf(event.songs.first())
        }

        return events.asReversed()
    }
}

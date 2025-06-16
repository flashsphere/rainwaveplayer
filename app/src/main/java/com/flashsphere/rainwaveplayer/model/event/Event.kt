package com.flashsphere.rainwaveplayer.model.event

import com.flashsphere.rainwaveplayer.model.song.Song
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = EventSerializer::class)
class Event(
    val id: Int,
    val name: String,
    val type: String,
    val stationId: Int,
    val startTime: Long,
    val endTime: Long,
    val votingAllowed: Boolean,
    var songs: List<Song> = emptyList(),
)

@Serializable
private class EventSurrogate(
    val id: Int = 0,
    val name: String = "",
    val type: String = "",
    val sid: Int = 0,
    val start_actual: Long = 0,
    val end: Long = 0,
    val voting_allowed: Boolean = false,
    var songs: List<Song> = emptyList(),
)

object EventSerializer : KSerializer<Event> {
    override val descriptor: SerialDescriptor = EventSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Event {
        val surrogate = decoder.decodeSerializableValue(EventSurrogate.serializer())

        surrogate.songs.forEach {
            it.votingAllowed = surrogate.voting_allowed
        }

        return Event(
            surrogate.id,
            surrogate.name,
            surrogate.type,
            surrogate.sid,
            surrogate.start_actual,
            surrogate.end,
            surrogate.voting_allowed,
            surrogate.songs,
        )
    }

    override fun serialize(encoder: Encoder, value: Event) {
    }
}

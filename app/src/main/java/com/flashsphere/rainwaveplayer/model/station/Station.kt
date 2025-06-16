package com.flashsphere.rainwaveplayer.model.station

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@Parcelize
@Immutable
class Station(
    val id: Int,
    val name: String,
    val stream: String,
    val description: String,
    val relays: List<String>,
) : Parcelable, Comparable<Station> {

    override fun compareTo(other: Station) = id.compareTo(other.id)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Station

        return id == other.id
    }

    override fun hashCode() = id

    override fun toString(): String {
        return "Station(id=$id, name='$name', stream='$stream', description='$description', relays=$relays)"
    }

    companion object {
        @JvmField
        val UNKNOWN = Station(0, "", "", "", emptyList())
    }
}

@Serializable
private class StationSurrogate(
    val id: Int = 0,
    val name: String = "",
    val stream: String = "",
    val description: String = "",
    val relays: List<RelaySurrogate> = emptyList(),
)

@Serializable
private class RelaySurrogate(
    val name: String = "",
    val protocol: String = "",
    val hostname: String = "",
    val port: Int = 0,
)

object StationSerializer : KSerializer<Station> {
    override val descriptor: SerialDescriptor = StationSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Station {
        val surrogate = decoder.decodeSerializableValue(StationSurrogate.serializer())
        return Station(
            surrogate.id,
            surrogate.name,
            surrogate.stream,
            surrogate.description,
            getRelays(surrogate),
        )
    }

    override fun serialize(encoder: Encoder, value: Station) {
    }

    private fun getRelays(surrogate: StationSurrogate): List<String> {
        if (surrogate.stream.isEmpty()) return emptyList()

        val streamUri = surrogate.stream
        if (surrogate.relays.isEmpty()) return listOf(streamUri)

        val relayUris = mutableListOf<String>()
        val filename = surrogate.stream.substringAfterLast("/", "")

        for (relay in surrogate.relays) {
            val url = StringBuilder()
                .append(relay.protocol)
                .append(relay.hostname)
                .append(relay.port
                    .takeIf { it > 0 }
                    ?.let { port ->
                        return@let if ((relay.protocol.startsWith("http") && port == 80)
                            || (relay.protocol.startsWith("https") && port == 443)) {
                            ""
                        } else {
                            ":$port"
                        }
                    } ?: "")
                .append("/")
                .append(filename)
                .toString()

            relayUris.add(url)
        }

        return relayUris
    }
}

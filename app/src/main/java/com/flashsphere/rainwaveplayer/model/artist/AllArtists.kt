package com.flashsphere.rainwaveplayer.model.artist

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = AllArtistsSerializer::class)
class AllArtists(
    val artists: List<Artist> = emptyList()
)

@Serializable
private class AllArtistsSurrogate(
    val all_artists: List<@Serializable(with = ArtistSerializer::class) Artist> = emptyList()
)

object AllArtistsSerializer : KSerializer<AllArtists> {
    override val descriptor: SerialDescriptor = AllArtistsSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): AllArtists {
        val surrogate = decoder.decodeSerializableValue(AllArtistsSurrogate.serializer())
        return AllArtists(
            surrogate.all_artists.sortedWith(ARTIST_NAME_COMPARATOR)
        )
    }

    override fun serialize(encoder: Encoder, value: AllArtists) {
    }
}

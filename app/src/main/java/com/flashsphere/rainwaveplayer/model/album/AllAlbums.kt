package com.flashsphere.rainwaveplayer.model.album

import androidx.collection.ScatterMap
import androidx.collection.mutableScatterMapOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = AllAlbumsSerializer::class)
data class AllAlbums(
    val albums: List<Album>,
    val albumMap: ScatterMap<Int, Album>,
)

@Serializable
private class AllAlbumsSurrogate(
    val all_albums: List<@Serializable(with = AlbumSerializer::class) Album> = emptyList()
)

object AllAlbumsSerializer : KSerializer<AllAlbums> {
    override val descriptor: SerialDescriptor = AllAlbumsSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): AllAlbums {
        val surrogate = decoder.decodeSerializableValue(AllAlbumsSurrogate.serializer())
        val list = surrogate.all_albums.sortedWith(ALBUM_NAME_COMPARATOR)
        val map = mutableScatterMapOf<Int, Album>().apply {
            surrogate.all_albums.forEach { this.put(it.id, it) }
        }
        return AllAlbums(list, map)
    }

    override fun serialize(encoder: Encoder, value: AllAlbums) {
    }
}

package com.flashsphere.rainwaveplayer.model.category

import com.flashsphere.rainwaveplayer.model.album.ALBUM_NAME_COMPARATOR
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.song.SONG_TITLE_COMPARATOR
import com.flashsphere.rainwaveplayer.model.song.Song
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale

val CATEGORY_NAME_COMPARATOR = compareBy<Category> { it.name.lowercase(Locale.ENGLISH) }.thenBy { it.id }

@Serializable(with = CategorySerializer::class)
class Category(
    internal val id: Int = 0,
    val name: String = "",
    val songs: Map<Album, List<Song>> = emptyMap(),
)

@Serializable
private class CategorySurrogate(
    val id: Int = 0,
    val name: String = "",
    val all_songs_for_sid: Map<String, List<Song>> = emptyMap(),
)

object CategorySerializer : KSerializer<Category> {
    override val descriptor: SerialDescriptor = CategorySurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Category {
        val surrogate = decoder.decodeSerializableValue(CategorySurrogate.serializer())
        return Category(
            surrogate.id,
            surrogate.name,
            groupSongs(surrogate),
        )
    }

    override fun serialize(encoder: Encoder, value: Category) {
    }

    private fun groupSongs(surrogate: CategorySurrogate): Map<Album, List<Song>> {
        if (surrogate.all_songs_for_sid.isEmpty()) return emptyMap()

        val groupedSongs: MutableMap<Album, List<Song>> = sortedMapOf(ALBUM_NAME_COMPARATOR)

        for ((albumIdStr, songs) in surrogate.all_songs_for_sid) {
            val albumId = albumIdStr.toIntOrNull() ?: continue
            val album = getAlbum(albumId, songs) ?: continue
            groupedSongs[album] = songs.sortedWith(SONG_TITLE_COMPARATOR)
        }

        return groupedSongs
    }

    private fun getAlbum(albumId: Int, songs: List<Song>): Album? {
        for (song in songs) {
            return song.albums.find { album -> album.id == albumId }
        }
        return null
    }
}

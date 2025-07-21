package com.flashsphere.rainwaveplayer.model.artist

import com.flashsphere.rainwaveplayer.model.album.ALBUM_NAME_COMPARATOR
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.song.SONG_TITLE_COMPARATOR
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.song.SongSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale
import java.util.SortedMap

val ARTIST_NAME_COMPARATOR = compareBy<Artist> { it.name.lowercase(Locale.ENGLISH) }.thenBy { it.id }

@Serializable
class Artist(
    val id: Int = 0,
    val name: String = "",
    val groupedSongs: Map<Int, Map<Album, List<Song>>> = emptyMap(),
)

@Serializable
private class ArtistSurrogate(
    val id: Int = 0,
    val name: String = "",
    val all_songs: Map<String, Map<String, List<@Serializable(with = SongSerializer::class) Song>>> = emptyMap(),
)

object ArtistSerializer : KSerializer<Artist> {
    override val descriptor: SerialDescriptor = ArtistSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Artist {
        val surrogate = decoder.decodeSerializableValue(ArtistSurrogate.serializer())
        return Artist(
            surrogate.id,
            surrogate.name,
            groupSongs(surrogate),
        )
    }

    override fun serialize(encoder: Encoder, value: Artist) {
    }

    private fun groupSongs(surrogate: ArtistSurrogate): Map<Int, Map<Album, List<Song>>> {
        if (surrogate.all_songs.isEmpty()) return emptyMap()

        val groupedSongs = hashMapOf<Int, SortedMap<Album, List<Song>>>()

        for ((stationIdStr, albumSongs) in surrogate.all_songs) {
            if (albumSongs.isEmpty()) continue

            val stationId = stationIdStr.toIntOrNull() ?: continue
            val sortedAlbumSongsMap: SortedMap<Album, List<Song>> = groupedSongs.getOrElse(stationId) {
                sortedMapOf(ALBUM_NAME_COMPARATOR)
            }

            for ((albumIdStr, songs) in albumSongs) {
                if (songs.isEmpty()) continue

                val albumId = albumIdStr.toIntOrNull() ?: continue
                val album = getAlbum(albumId, songs) ?: continue

                sortedAlbumSongsMap[album] = songs.sortedWith(SONG_TITLE_COMPARATOR)
            }
            groupedSongs[stationId] = sortedAlbumSongsMap
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

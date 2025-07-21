package com.flashsphere.rainwaveplayer.model.song

import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.album.AlbumSerializer
import com.flashsphere.rainwaveplayer.model.artist.Artist
import com.flashsphere.rainwaveplayer.model.artist.ArtistSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale

val SONG_TITLE_COMPARATOR = compareBy<Song> { it.title.lowercase(Locale.ENGLISH) }.thenBy { it.id }

@Serializable
class Song(
    val id: Int = 0,
    val title: String,
    val rating: Float,
    var ratingUser: Float,
    val artists: List<Artist>,
    val albums: List<Album>,
    val ratingAllowed: Boolean,
    val entryId: Int,
    val cool: Boolean,
    val requestable: Boolean,
    var favorite: Boolean,
    val userIdRequested: Int,
    val usernameRequested: String,
    val stationId: Int,
    var voted: Boolean,
    var votingAllowed: Boolean,
    val length: Long
) {

    fun getAlbumName() = albums.asSequence()
        .map { it.name }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

    fun getArtistName() = artists.asSequence()
        .map { it.name }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

    fun getAlbumCoverUrl() = albums[0].getArtUrl()
}

@Serializable
private class SongSurrogate(
    val id: Int = 0,
    val title: String = "",
    val rating: Float = 0F,
    val rating_user: Float = 0F,
    val artists: List<@Serializable(with = ArtistSerializer::class) Artist> = emptyList(),
    val albums: List<@Serializable(with = AlbumSerializer::class) Album> = emptyList(),
    val rating_allowed: Boolean = false,
    val entry_id: Int = 0,
    val cool: Boolean = false,
    val requestable: Boolean = false,
    val fave: Boolean = false,
    val elec_request_user_id: Int = 0,
    val elec_request_username: String = "",
    val sid: Int? = null,
    val origin_sid: Int? = null,
    val album_id: Int = 0,
    val album_name: String = "",
    val length: Long = 0,
)

object SongSerializer : KSerializer<Song> {
    override val descriptor: SerialDescriptor = SongSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Song {
        val surrogate = decoder.decodeSerializableValue(SongSurrogate.serializer())

        var albums = surrogate.albums
        if (albums.isEmpty() && surrogate.album_name.isNotEmpty()) {
            albums = listOf(Album(
                id = surrogate.album_id,
                name = surrogate.album_name,
            ))
        }

        return Song(
            surrogate.id,
            surrogate.title,
            surrogate.rating,
            surrogate.rating_user,
            surrogate.artists,
            albums,
            surrogate.rating_allowed,
            surrogate.entry_id,
            surrogate.cool,
            surrogate.requestable,
            surrogate.fave,
            surrogate.elec_request_user_id,
            surrogate.elec_request_username,
            surrogate.sid ?: surrogate.origin_sid ?: 0,
            false,
            false,
            surrogate.length,
        )
    }

    override fun serialize(encoder: Encoder, value: Song) {
    }
}

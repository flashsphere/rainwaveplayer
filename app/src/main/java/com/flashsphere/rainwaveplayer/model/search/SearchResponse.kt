package com.flashsphere.rainwaveplayer.model.search

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.album.AlbumSerializer
import com.flashsphere.rainwaveplayer.model.artist.Artist
import com.flashsphere.rainwaveplayer.model.artist.ArtistSerializer
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.song.SongSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SearchResponseSerializer::class)
class SearchResponse(
    override val result: ResponseResult?,
    val artists: List<Artist>,
    val albums: List<Album>,
    val songs: List<Song>,
) : HasResponseResult<ResponseResult>

@Serializable
private class SearchResponseSurrogate(
    val search_results: ResponseResult? = null,
    val artists: List<@Serializable(with = ArtistSerializer::class) Artist> = emptyList(),
    val albums: List<@Serializable(with = AlbumSerializer::class) Album> = emptyList(),
    val songs: List<@Serializable(with = SongSerializer::class) Song> = emptyList(),
)

object SearchResponseSerializer : KSerializer<SearchResponse> {
    override val descriptor: SerialDescriptor = SearchResponseSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): SearchResponse {
        val surrogate = decoder.decodeSerializableValue(SearchResponseSurrogate.serializer())
        return SearchResponse(
            surrogate.search_results,
            surrogate.artists,
            surrogate.albums,
            surrogate.songs,
        )
    }

    override fun serialize(encoder: Encoder, value: SearchResponse) {
    }
}

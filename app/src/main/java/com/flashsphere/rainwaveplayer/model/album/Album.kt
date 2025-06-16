package com.flashsphere.rainwaveplayer.model.album

import com.flashsphere.rainwaveplayer.model.song.SONG_TITLE_COMPARATOR
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonTransformingSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.max

val ALBUM_NAME_COMPARATOR = compareBy<Album> { it.name.lowercase(Locale.ENGLISH) }.thenBy { it.id }

const val ART_PATH_SUFFIX = "_320.jpg"
const val NO_ART_PATH = "/static/images4/noart_1.jpg"

@Serializable(with = AlbumSerializer::class)
class Album(
    internal val id: Int = 0,
    val name: String = "",
    val art: String = "",
    val rating: Float = 0F,
    val ratingUser: Float = 0F,
    val cool: Boolean = false,
    val ratingCount: Int = 0,
    val songs: List<Song> = emptyList(),
    var favorite: Boolean = false,
    val ratingDistribution: Map<Int, Float> = emptyMap(),
) {
    fun getArtUrl() = RainwaveService.BASE_URL + art
}

@Serializable
private class AlbumSurrogate(
    val id: Int = 0,
    val name: String = "",
    val art: String = "",
    val rating: Float = 0F,
    val rating_user: Float = 0F,
    val cool: Boolean = false,
    val rating_histogram: Map<String, Int> = emptyMap(),
    val rating_count: Int = 0,
    @Serializable(with = AlbumSongListSerializer::class)
    val songs: List<Song> = emptyList(),
    val fave: Boolean = false,
)

object AlbumSongListSerializer : JsonTransformingSerializer<List<Song>>(ListSerializer(Song.serializer()))

object AlbumSerializer : KSerializer<Album> {
    override val descriptor: SerialDescriptor = AlbumSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Album {
        val surrogate = decoder.decodeSerializableValue(AlbumSurrogate.serializer())
        return Album(
            surrogate.id,
            surrogate.name,
            getArt(surrogate),
            surrogate.rating,
            surrogate.rating_user,
            surrogate.cool,
            surrogate.rating_count,
            surrogate.songs.sortedWith(SONG_TITLE_COMPARATOR),
            surrogate.fave,
            getRatingDistribution(surrogate),
        )
    }

    override fun serialize(encoder: Encoder, value: Album) {
    }

    private fun getArt(surrogate: AlbumSurrogate): String {
        if (surrogate.art.isNotEmpty()) {
            return surrogate.art + ART_PATH_SUFFIX
        }
        return NO_ART_PATH
    }

    private fun getRatingDistribution(surrogate: AlbumSurrogate): Map<Int, Float> {
        val ratingDistribution = hashMapOf<Int, Float>()
        for (i in 1..5) {
            ratingDistribution[i] = 0F
        }

        if (surrogate.rating_histogram.isEmpty()) {
            return ratingDistribution
        }

        var highestCount = 0
        val aggregated = hashMapOf<Int, Int>().withDefault { 0 }

        surrogate.rating_histogram.forEach { (ratingStr, count) ->
            val rating = ratingStr.toBigDecimalOrNull() ?: return@forEach

            val ratingInt = rating.toInt()
            val total = count + aggregated.getValue(ratingInt)
            aggregated[ratingInt] = total

            highestCount = max(total, highestCount)
        }

        for (i in 1..5) {
            if (aggregated.containsKey(i)) {
                ratingDistribution[i] = aggregated.getValue(i).toBigDecimal().divide(BigDecimal(highestCount), 2, RoundingMode.DOWN).toFloat()
            }
        }
        return ratingDistribution
    }
}

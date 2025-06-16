package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.album.Album
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Request(
    @SerialName("request_id")
    val requestId: Int = 0,

    @SerialName("id")
    val songId: Int = 0,

    @SerialName("origin_sid")
    val stationId: Int = 0,

    @SerialName("title")
    val title: String = "",

    @SerialName("valid")
    val valid: Boolean = false,

    @SerialName("good")
    val good: Boolean = false,

    @SerialName("albums")
    val albums: List<Album> = emptyList(),

    @SerialName("rating")
    val rating: Float = 0F,

    @SerialName("rating_user")
    val ratingUser: Float = 0F,

    @SerialName("fave")
    val favorite: Boolean = false,

    @SerialName("cool")
    val cool: Boolean = false,

    @SerialName("cool_end")
    val coolEndTime: Long = 0,

    @SerialName("elec_blocked")
    val electionBlocked: Boolean = false,

    @SerialName("elec_blocked_by")
    val electionBlockedBy: String = "",

    @Transient
    var stationName: String = "",
) {
    fun getAlbumName() = albums
        .map { it.name }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

    fun getAlbumCoverUrl() = albums[0].getArtUrl()
}

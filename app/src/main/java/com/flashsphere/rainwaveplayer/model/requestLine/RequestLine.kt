package com.flashsphere.rainwaveplayer.model.requestLine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val POSITION_COMPARATOR = compareBy<RequestLine> { it.position }

@Serializable
class RequestLine(
    @SerialName("username")
    val username: String = "",

    @SerialName("user_id")
    val userId: Int = 0,

    @SerialName("song")
    val song: RequestLineSong? = null,

    @SerialName("position")
    val position: Int = 0,
)

@Serializable
class RequestLineSong(
    @SerialName("id")
    val id: Int = 0,

    @SerialName("album_name")
    val albumName: String = "",

    @SerialName("title")
    val title: String = "",
)

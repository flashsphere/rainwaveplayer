package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.model.requestLine.RequestLine

@Immutable
class RequestLineState(
    val userId: Int,
    val username: String,
    val position: Int,
    val albumName: String?,
    val songTitle: String?,
) : LibraryItem {
    override val id: Int = userId
    override val name: String = username
    override val searchable: String = username

    constructor(requestLine: RequestLine) : this(
        userId = requestLine.userId,
        username = requestLine.username,
        position = requestLine.position,
        albumName = requestLine.song?.albumName,
        songTitle = requestLine.song?.title
    )
}

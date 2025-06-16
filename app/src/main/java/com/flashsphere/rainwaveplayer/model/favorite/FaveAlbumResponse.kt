package com.flashsphere.rainwaveplayer.model.favorite

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FaveAlbumResponse(
    @SerialName("fave_album_result")
    override val result: FaveResult
) : HasResponseResult<FaveResult>

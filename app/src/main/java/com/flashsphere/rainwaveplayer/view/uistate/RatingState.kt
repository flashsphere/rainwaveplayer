package com.flashsphere.rainwaveplayer.view.uistate

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RatingState(
    val songId: Int,
    val songTitle: String,
    val rating: Float,
) : Parcelable

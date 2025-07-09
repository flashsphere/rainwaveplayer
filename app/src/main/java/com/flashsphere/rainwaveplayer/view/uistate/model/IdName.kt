package com.flashsphere.rainwaveplayer.view.uistate.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IdName(val id: Int, val name: String) : Parcelable

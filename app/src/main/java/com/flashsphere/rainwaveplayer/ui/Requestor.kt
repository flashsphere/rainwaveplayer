package com.flashsphere.rainwaveplayer.ui

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.flashsphere.rainwaveplayer.R

interface Requestor {
    operator fun component1(): Int = bgColor
    operator fun component2(): Int = textColor
    operator fun component3(): Int = text

    @get:ColorRes
    val bgColor: Int
    @get:ColorRes
    val textColor: Int
    @get:StringRes
    val text: Int
}

object SelfRequestor : Requestor {
    override val bgColor: Int = R.color.self_requestor_background
    override val textColor: Int = R.color.self_requestor_text
    override val text: Int = R.string.yours
}

object OtherRequestor : Requestor {
    override val bgColor: Int = R.color.other_requestor_background
    override val textColor: Int = R.color.other_requestor_text
    override val text: Int = R.string.request
}

package com.flashsphere.rainwaveplayer.ui

import androidx.annotation.StringRes
import com.flashsphere.rainwaveplayer.R

enum class LibraryType(
    @StringRes val stringResId: Int
) {
    Albums(R.string.albums),
    Artists(R.string.artists),
    Categories(R.string.categories),
    RequestLine(R.string.request_line),
}

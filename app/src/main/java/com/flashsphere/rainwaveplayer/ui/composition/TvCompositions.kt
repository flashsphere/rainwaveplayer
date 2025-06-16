package com.flashsphere.rainwaveplayer.ui.composition

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.tv.material3.DrawerState
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.HIDE_RATING_UNTIL_RATED
import com.flashsphere.rainwaveplayer.util.getBlocking
import kotlinx.parcelize.Parcelize

val LocalLastFocused = compositionLocalOf<MutableState<LastFocused>> {
    error("LastFocused not provided")
}

val LocalDrawerState = compositionLocalOf<DrawerState> {
    error("LocalDrawerState not provided")
}

val LocalTvUiSettings = staticCompositionLocalOf<TvUiSettings> {
    error("LocalTvUiSettings not provided")
}

data class TvUiSettings(
    val hideRatingsUntilRated: Boolean,
) {
    constructor(dataStore: DataStore<Preferences>) : this(
        hideRatingsUntilRated = dataStore.getBlocking(HIDE_RATING_UNTIL_RATED),
    )
}

@Parcelize
@Immutable
class LastFocused(
    val tag: String? = null,
    val shouldRequestFocus: Boolean = false,
): Parcelable {
    fun copy(tag: String?): LastFocused {
        return LastFocused(tag, this.shouldRequestFocus)
    }
    fun copy(shouldRequestFocus: Boolean): LastFocused {
        return LastFocused(this.tag, shouldRequestFocus)
    }
    override fun toString(): String {
        return "LastFocused(tag=$tag, shouldRequestFocus=$shouldRequestFocus)"
    }
}

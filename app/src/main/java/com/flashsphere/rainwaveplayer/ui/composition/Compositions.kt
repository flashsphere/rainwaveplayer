package com.flashsphere.rainwaveplayer.ui.composition

import android.content.res.Configuration
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.util.BottomNavPreference
import com.flashsphere.rainwaveplayer.util.PreferencesKeys
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.HIDE_RATING_UNTIL_RATED
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.util.getBlocking

val LocalUiSettings = staticCompositionLocalOf<UiSettings> {
    error("LocalUiSettings not provided")
}

data class UiSettings(
    val navigationSuiteType: NavigationSuiteType,
    val bottomNavPreference: BottomNavPreference,
    val hideRatingsUntilRated: Boolean,
) {
    constructor(navigationSuiteType: NavigationSuiteType, dataStore: DataStore<Preferences>) : this(
        navigationSuiteType = navigationSuiteType,
        bottomNavPreference = BottomNavPreference.of(dataStore.getBlocking(PreferencesKeys.BTM_NAV)),
        hideRatingsUntilRated = dataStore.getBlocking(HIDE_RATING_UNTIL_RATED),
    )

    companion object {
        fun from(
            navigationSuiteType: NavigationSuiteType = NavigationSuiteType.None,
            bottomNavPreference: BottomNavPreference = BottomNavPreference.Labeled,
            hideRatingsUntilRated: Boolean = false
        ) = UiSettings(navigationSuiteType, bottomNavPreference, hideRatingsUntilRated)
    }
}

val LocalUserCredentials = staticCompositionLocalOf<UserCredentials?> {
    null
}

val LocalUiScreenConfig = staticCompositionLocalOf<UiScreenConfig> {
    error("LocalUiScreenConfig not provided")
}

@Stable
class UiScreenConfig(
    val windowSize: DpSize = DpSize(0.dp, 0.dp),
    val widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    val heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Compact,
) {
    val listItemLineHeight: Dp = 48.dp
    val listItemPadding: Dp = 16.dp

    val gridSpan: Int = when {
        (widthSizeClass == WindowWidthSizeClass.Compact ||
            heightSizeClass == WindowHeightSizeClass.Compact ||
            windowSize.width < 825.dp) -> 1
        else -> 2
    }

    val songItemTitleLineHeight: TextUnit = 20.sp
    val songItemAlbumLineHeight: TextUnit = 18.sp
    val songItemArtistLineHeight: TextUnit = 16.sp
    val songItemCooldownLineHeight: TextUnit = 18.sp

    val nowPlayingImageSize: Dp = (windowSize.width / gridSpan).let { width ->
        when {
            (width >= 600.dp) -> 130.dp
            (width >= 380.dp) -> 120.dp
            else -> 110.dp
        }
    }
    val albumDetailImageSize: Dp = when {
        (windowSize.width >= 600.dp) -> 150.dp
        (windowSize.width >= 380.dp) -> 140.dp
        else -> 120.dp
    }
    val albumRatingHistogramSize: Dp = when {
        (windowSize.width >= 600.dp) -> 400.dp
        (windowSize.width >= 380.dp) -> 390.dp
        else -> 0.dp
    }
    val itemPadding: Dp = 8.dp
    val tvCardWidth = (windowSize.width - 120.dp - 16.dp) / 3

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    constructor(configuration: Configuration) : this(
        configuration = configuration,
        windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)),
    )

    constructor(configuration: Configuration, windowSizeClass: WindowSizeClass) : this(
        windowSize = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp),
        widthSizeClass = windowSizeClass.widthSizeClass,
        heightSizeClass = windowSizeClass.heightSizeClass,
    )
}

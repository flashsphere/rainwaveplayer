package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.composition.TvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiSettings
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlaying
import com.flashsphere.rainwaveplayer.ui.navigation.rememberNavigationState
import com.flashsphere.rainwaveplayer.ui.navigation.topLevelRoutes
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTheme
import com.flashsphere.rainwaveplayer.util.BottomNavPreference
import com.flashsphere.rainwaveplayer.util.UserCredentials

@Preview(device = Devices.NEXUS_5, apiLevel = 34)
annotation class Preview

@Preview(device = Devices.PIXEL_TABLET, apiLevel = 34)
annotation class PreviewTablet

// TV_720p,TV_1080p device previews are broken as it shows up as portrait instead of landscape
@Preview(device = "spec:width=1280dp,height=720dp,dpi=420", apiLevel = 34)
annotation class PreviewTv

@Composable
fun PreviewTheme(
    userCredentials: UserCredentials? = null,
    navSuiteType: NavigationSuiteType = NavigationSuiteType.NavigationBar,
    bottomNavUiPref: BottomNavPreference = BottomNavPreference.Labeled,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalUiScreenConfig provides UiScreenConfig(LocalConfiguration.current),
        LocalUserCredentials provides userCredentials,
        LocalUiSettings provides UiSettings.from(navSuiteType, bottomNavUiPref),
    ) {
        AppTheme {
            content()
        }
    }
}

@Composable
fun PreviewTvTheme(
    userCredentials: UserCredentials? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLastFocused provides remember { mutableStateOf(LastFocused()) },
        LocalUserCredentials provides userCredentials,
        LocalUiScreenConfig provides UiScreenConfig(LocalConfiguration.current),
        LocalTvUiSettings provides TvUiSettings(hideRatingsUntilRated = false),
    ) {
        TvAppTheme {
            content()
        }
    }
}

@Composable
fun rememberPreviewNavigator(): Navigator {
    return Navigator(rememberNavigationState(NowPlaying, topLevelRoutes))
}

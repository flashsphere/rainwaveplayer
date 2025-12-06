package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import com.flashsphere.rainwaveplayer.R
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface Route : NavKey

interface TopLevelRoute : Route {
    @get:StringRes
    val title: Int
    @get:DrawableRes
    val icon: Int
}

interface DetailRoute : Route {
    val id: Int
    val name: String
}

@Serializable
object NowPlaying : TopLevelRoute {
    override val title: Int = R.string.now_playing
    override val icon: Int = R.drawable.ic_now_playing_white_24dp
}
@Serializable object Requests : TopLevelRoute {
    override val title: Int = R.string.action_requests
    override val icon: Int = R.drawable.ic_requests_white_24dp
}
@Serializable object Library : TopLevelRoute {
    override val title: Int = R.string.action_library
    override val icon: Int = R.drawable.ic_library_music_white_24dp
}
@Serializable object Search : TopLevelRoute {
    override val title: Int = R.string.action_search
    override val icon: Int = R.drawable.ic_search_white_24dp
}

@Serializable data class AlbumDetail(override val id: Int, override val name: String): DetailRoute
@Serializable data class ArtistDetail(override val id: Int, override val name: String): DetailRoute
@Serializable data class CategoryDetail(override val id: Int, override val name: String): DetailRoute

val topLevelRoutes = setOf(NowPlaying, Requests, Library, Search)
val drawerGesturesEnabledTopLevelRoutes = setOf(NowPlaying, Requests, Library)

@Composable
fun Routes(
    navigator: Navigator,
    scrollToTop: MutableState<Boolean>,
    itemContent: @Composable (route: TopLevelRoute, selected: Boolean, onClick: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()

    topLevelRoutes.forEachIndexed { i, route ->
        key(i) {
            itemContent(route, navigator.state.topLevelRoute == route) {
                val containsCurrentRoute = navigator.state.topLevelRoute == route
                val isRouteOnTop = if (navigator.state.topLevelRoute == route) {
                    navigator.state.backStacks[route]?.size == 1
                } else {
                    false
                }

                if (isRouteOnTop) {
                    scope.launch { scrollToTop.value = true }
                } else if (containsCurrentRoute) {
                    navigator.goBackToTopLevel()
                } else {
                    navigator.navigate(route)
                }
            }
        }
    }
}

package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.flashsphere.rainwaveplayer.R
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

val topLevelRoutes = listOf(NowPlayingRoute, RequestsRoute, LibraryRoute, SearchRoute)
val drawerGesturesEnabledTopLevelRoutes = listOf(NowPlayingRoute, RequestsRoute, LibraryRoute)

interface Route {
    @get:StringRes
    val title: Int
    @get:DrawableRes
    val icon: Int
    val startDestination: Any
}

interface DetailRoute

@Serializable
object NowPlayingRoute : Route {
    override val title: Int = R.string.now_playing
    override val icon: Int = R.drawable.ic_now_playing_white_24dp
    override val startDestination = this
}
@Serializable object RequestsRoute : Route {
    override val title: Int = R.string.action_requests
    override val icon: Int = R.drawable.ic_requests_white_24dp
    override val startDestination = this
}
@Serializable object LibraryRoute : Route {
    override val title: Int = R.string.action_library
    override val icon: Int = R.drawable.ic_library_music_white_24dp
    override val startDestination = Library
}
@Serializable object SearchRoute : Route {
    override val title: Int = R.string.action_search
    override val icon: Int = R.drawable.ic_search_white_24dp
    override val startDestination = Search
}

@Serializable object Library: DetailRoute
@Serializable object Search: DetailRoute
@Serializable data class AlbumDetail(val id: Int, val name: String): DetailRoute
@Serializable data class ArtistDetail(val id: Int, val name: String): DetailRoute
@Serializable data class CategoryDetail(val id: Int, val name: String): DetailRoute

@Composable
fun Routes(
    navController: NavHostController,
    scrollToTop: MutableState<Boolean>,
    itemContent: @Composable (route: Route, selected: Boolean, onClick: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()

    topLevelRoutes.forEachIndexed { i, route ->
        key(i) {
            var containsCurrentRoute by remember { mutableStateOf(false) }
            var isRouteOnTop by remember { mutableStateOf(false) }

            LaunchedEffect(route) {
                navController.currentBackStackEntryFlow
                    .map { it.destination }
                    .collect { destination ->
                        containsCurrentRoute = destination.hierarchy.any { it.hasRoute(route::class) }
                        isRouteOnTop = destination.hasRoute(route.startDestination::class)
                    }
            }

            itemContent(route, containsCurrentRoute) {
                if (isRouteOnTop) {
                    scope.launch { scrollToTop.value = true }
                } else if (containsCurrentRoute) {
                    navController.popBackStack(route.startDestination, false)
                } else {
                    navController.navigateToRoute(route)
                }
            }
        }
    }
}

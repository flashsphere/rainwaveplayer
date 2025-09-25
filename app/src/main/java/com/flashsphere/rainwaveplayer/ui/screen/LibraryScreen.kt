package com.flashsphere.rainwaveplayer.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.LibraryType
import com.flashsphere.rainwaveplayer.ui.MenuIcon
import com.flashsphere.rainwaveplayer.ui.SearchTextField
import com.flashsphere.rainwaveplayer.ui.SearchTextFieldLabel
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarAction
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarActions
import com.flashsphere.rainwaveplayer.ui.appbar.toAppBarAction
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.enterAlwaysScrollBehavior
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlayingRoute
import com.flashsphere.rainwaveplayer.ui.navigation.RequestsRoute
import com.flashsphere.rainwaveplayer.ui.navigation.SearchRoute
import com.flashsphere.rainwaveplayer.view.viewmodel.LibraryScreenViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavHostController,
    viewModel: LibraryScreenViewModel,
    stationFlow: StateFlow<Station?>,
    onMenuClick: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return
    val tabs = remember { LibraryType.entries.toTypedArray() }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val filterActivated = rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = enterAlwaysScrollBehavior(
        scrollToTop = scrollToTop,
        canScroll = { !filterActivated.value }
    )

    LaunchedEffect(filterActivated.value) {
        if (filterActivated.value) {
            scrollBehavior.state.heightOffset = 0F
        } else {
            viewModel.resetFilter()
        }
    }

    BackHandler(filterActivated.value) {
        filterActivated.value = false
    }

    AppScaffold(
        navigationIcon = {
            if (filterActivated.value) {
                BackIcon(onBackClick = { filterActivated.value = false })
            } else if (LocalUiSettings.current.navigationSuiteType == NavigationSuiteType.None) {
                BackIcon(onBackClick = { navController.popBackStack() })
            } else {
                MenuIcon(onMenuClick = onMenuClick)
            }
        },
        appBarContent = {
            if (filterActivated.value) {
                SearchTextField(
                    state = viewModel.filterTextFieldState,
                    onSubmit = { viewModel.submitFilter() },
                    label = {
                        SearchTextFieldLabel(
                            painter = rememberVectorPainter(Icons.Filled.FilterList),
                            text = stringResource(id = R.string.action_filter)
                        )
                    },
                )
            } else {
                AppBarTitle(
                    title = stringResource(id = R.string.action_library),
                    subtitle = station.name,
                    onClick = { scope.launch { scrollToTop.value = true } },
                )
            }
        },
        appBarScrollBehavior = scrollBehavior,
        appBarActions = {
            if (LocalUiSettings.current.navigationSuiteType == NavigationSuiteType.None) {
                val actions = mutableListOf<AppBarAction>()
                val overflowActions = mutableListOf<AppBarAction>()

                if (filterActivated.value) {
                    overflowActions.addAll(toAppBarAction(navController,
                        listOf(NowPlayingRoute, RequestsRoute, SearchRoute)))
                } else {
                    actions.addAll(toAppBarAction(navController,
                        listOf(NowPlayingRoute, RequestsRoute, SearchRoute)))
                }
                AppBarActions(
                    actions = actions,
                    overflowActions = overflowActions,
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !filterActivated.value,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    onClick = { filterActivated.value = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,) {
                    Icon(imageVector = Icons.Filled.FilterList,
                        stringResource(id = R.string.action_filter))
                }
            }
        },
        snackbarEvents = viewModel.snackbarEvents,
    ) {
        Column {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                edgePadding = 0.dp,
            ) {
                tabs.forEachIndexed { i, entry ->
                    key(i) {
                        Tab(selected = pagerState.currentPage == i,
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            text = { Text(text = stringResource(id = entry.stringResId)) },
                            onClick = {
                                if (pagerState.currentPage == i) {
                                    scope.launch { scrollToTop.value = true }
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(i) }
                                }
                            })
                    }
                }
            }

            HorizontalPager(state = pagerState, key = { tabs[it] },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) { index ->
                when (tabs[index]) {
                    LibraryType.Albums -> {
                        AlbumLibraryTab(
                            navController = navController,
                            station = station,
                            viewModel = viewModel,
                            scrollToTop = scrollToTop,
                        )
                    }
                    LibraryType.Artists -> {
                        ArtistLibraryTab(
                            navController = navController,
                            station = station,
                            viewModel = viewModel,
                            scrollToTop = scrollToTop,
                        )
                    }
                    LibraryType.Categories -> {
                        CategoryLibraryTab(
                            navController = navController,
                            station = station,
                            viewModel = viewModel,
                            scrollToTop = scrollToTop,
                        )
                    }
                    LibraryType.RequestLine -> {
                        RequestLineLibraryTab(
                            station = station,
                            viewModel = viewModel,
                            scrollToTop = scrollToTop,
                        )
                    }
                }
            }
        }
    }
}

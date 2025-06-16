package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.LibraryType
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.view.viewmodel.LibraryScreenViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TvLibraryScreen(
    navController: NavHostController,
    viewModel: LibraryScreenViewModel,
    stationFlow: StateFlow<Station?>,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return
    val selectedLibrary = rememberSaveable { mutableStateOf(LibraryType.Albums) }

    val lastFocused = LocalLastFocused.current
    LifecycleStartEffect(Unit) {
        if (lastFocused.value.tag == null) {
            lastFocused.value = LastFocused("tab_row", true)
        } else {
            lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
        }
        onStopOrDispose {}
    }
    Surface {
        Column(modifier = Modifier.fillMaxSize().padding(start = 80.dp, end = 40.dp)) {
            LibraryTabRow(
                selectedLibrary = selectedLibrary.value,
                onLibrarySelected = { selectedLibrary.value = it },
            )

            when(selectedLibrary.value) {
                LibraryType.Albums -> TvAlbumLibraryTab(
                    navController = navController,
                    viewModel = viewModel,
                    station = station,
                )
                LibraryType.Artists ->  TvArtistLibraryTab(
                    navController = navController,
                    viewModel = viewModel,
                    station = station,
                )
                LibraryType.Categories -> TvCategoryLibraryTab(
                    navController = navController,
                    viewModel = viewModel,
                    station = station,
                )
                LibraryType.RequestLine -> TvRequestLineLibraryTab(
                    viewModel = viewModel,
                    station = station,
                )
            }
        }
    }
}

@Composable
private fun LibraryTabRow(
    selectedLibrary: LibraryType,
    onLibrarySelected: (type: LibraryType) -> Unit,
) {
    val tabs = remember { LibraryType.entries.toTypedArray() }
    val selectedTabIndex = tabs.indexOf(selectedLibrary)
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember(tabs) {
        mutableMapOf<Int, FocusRequester>().also {
            tabs.forEachIndexed { i, _ ->
                it[i] = FocusRequester()
            }
        }
    }

    val tabRowFocusRequester = remember { FocusRequester() }
    val hasFocus = remember { mutableStateOf(false) }
    BackHandler(!hasFocus.value) {
        tabRowFocusRequester.requestFocus()
    }

    TabRow(
        modifier = Modifier.fillMaxWidth().focusGroup().padding(top = 20.dp, bottom = 10.dp)
            .focusRestorer(focusRequesters[selectedTabIndex] ?: Default)
            .saveLastFocused("tab_row", tabRowFocusRequester)
            .onFocusChanged {
                hasFocus.value = it.hasFocus
            },
        selectedTabIndex = selectedTabIndex,
        separator = { Spacer(modifier = Modifier) },
        indicator = { tabPositions, doesTabRowHaveFocus ->
            tabPositions.getOrNull(selectedTabIndex)?.let { currentTabPosition ->
                TabRowDefaults.PillIndicator(
                    currentTabPosition = currentTabPosition,
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                    inactiveColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            }
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            key(index) {
                Tab(
                    modifier = Modifier.focusRequester(focusRequesters[index]!!),
                    selected = selectedTabIndex == index,
                    onFocus = { onLibrarySelected(tab) },
                    onClick = { focusManager.moveFocus(FocusDirection.Down) },
                    colors = TabDefaults.pillIndicatorTabColors(
                        selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                ) {
                    Text(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = stringResource(tab.stringResId),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LocalContentColor.current
                        )
                    )
                }
            }
        }
    }
}

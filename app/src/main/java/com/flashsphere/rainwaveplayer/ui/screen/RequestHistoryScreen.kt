package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleStartEffect
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.view.viewmodel.UserPagedListViewModel

@Composable
fun RequestHistoryScreen(viewModel: UserPagedListViewModel, onBackPress: () -> Unit) {
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        viewModel.getStations()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }
    AppTheme {
        PagedSongsWithStationSelector(
            flow = viewModel.requestHistory,
            stationsScreenStateFlow = viewModel.stationsScreenState,
            title = stringResource(id = R.string.request_history),
            stationFlow = viewModel.station,
            onStationSelected = { viewModel.station(it) },
            events = viewModel.snackbarEvents,
            onFaveClick = viewModel::faveSong,
            onStationsRetry = { viewModel.getStations() },
            onBackPress = onBackPress,
        )
    }
}

package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleStartEffect
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.view.viewmodel.UserPagedListViewModel

@Composable
fun AllFavesScreen(viewModel: UserPagedListViewModel, onBackPress: () -> Unit) {
    AppTheme {
        LifecycleStartEffect(Unit) {
            viewModel.subscribeStateChangeEvents()
            onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
        }
        PagedSongs(flow = viewModel.allFaves,
            events = viewModel.snackbarEvents,
            onFaveClick = viewModel::faveSong,
            onBackPress = onBackPress
        )
    }
}

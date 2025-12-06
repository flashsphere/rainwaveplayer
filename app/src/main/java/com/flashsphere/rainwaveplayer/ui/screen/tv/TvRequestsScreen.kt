package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.TvErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.animation.fadeIn
import com.flashsphere.rainwaveplayer.ui.animation.fadeOut
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.item.tv.TvRequestSongCard
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.requestStateData
import com.flashsphere.rainwaveplayer.ui.screen.userStateData
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.RequestsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import com.flashsphere.rainwaveplayer.view.viewmodel.RequestsScreenViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.min

@Composable
fun TvRequestsScreen(
    navigator: Navigator,
    viewModel: RequestsScreenViewModel,
    stationFlow: StateFlow<Station?>,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value
    LifecycleStartEffect(station) {
        if (station == null) {
            navigator.goBack()
        } else {
            viewModel.subscribeStationInfo(station, false)
        }
        onStopOrDispose { viewModel.unsubscribeStationInfo() }
    }
    if (station == null) {
        return
    }

    val screenState = viewModel.requestsScreenState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val selectedRequest = remember { mutableStateOf<SelectedRequest?>(null) }

    val lastFocused = LocalLastFocused.current
    LifecycleStartEffect(Unit) {
        if (lastFocused.value.tag == null) {
            lastFocused.value = LastFocused("suspend_resume_btn", true)
        } else if (selectedRequest.value == null) {
            lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
        }

        onStopOrDispose {}
    }

    TvRequestsScreen(
        screenState = screenState,
        onRetry = { viewModel.subscribeStationInfo(true) },
        onResumeClick = {
            viewModel.showSnackbarMessage(context.getString(R.string.loading_resume_queue))
            viewModel.resumeQueue()
        },
        onSuspendClick = {
            viewModel.showSnackbarMessage(context.getString(R.string.loading_suspend_queue))
            viewModel.suspendQueue()
        },
        onRequestFavesClick = {
            viewModel.showSnackbarMessage(context.getString(R.string.loading_request_fave))
            viewModel.requestFavorites()
        },
        onRequestUnratedClick = {
            viewModel.showSnackbarMessage(context.getString(R.string.loading_request_unrated))
            viewModel.requestUnrated()
        },
        onClearClick = {
            viewModel.showSnackbarMessage(context.getString(R.string.loading_clear_request))
            viewModel.clearRequests()
        },
        onRefreshClick = {
            viewModel.showSnackbarMessage(context.getString(R.string.refreshing))
            viewModel.subscribeStationInfo(true)
        },
        selectedRequest = selectedRequest,
    )

    AnimatedVisibility(
        visible = selectedRequest.value != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        selectedRequest.value?.let {
            TvRequestActionsScreen(
                viewModel = viewModel,
                selectedRequest = it.request,
                onDismissRequest = { selectedRequest.value = null },
            )
            DisposableEffect(Unit) {
                var tag = lastFocused.value.tag
                onDispose {
                    val requests = screenState.requests?.toList() ?: emptyList()

                    // if requests are empty, we focus on the action buttons
                    if (requests.isEmpty()) {
                        tag = "suspend_resume_btn"
                    } else {
                        val found = requests.any { r -> r.songId == it.request.songId }
                        // if selected request is no longer in the list
                        if (!found) {
                            // we focus on the current request at the same index
                            val index = min(it.index, requests.size - 1)
                            tag = "request_${requests[index].songId}"
                        }
                    }

                    lastFocused.value = LastFocused(tag = tag, shouldRequestFocus = true)
                }
            }
        }
    }
}

private const val gridColumnCount = 3

@Composable
private fun TvRequestsScreen(
    screenState: RequestsScreenState,
    onRetry: () -> Unit,
    onResumeClick: () -> Unit,
    onSuspendClick: () -> Unit,
    onRequestFavesClick: () -> Unit,
    onRequestUnratedClick: () -> Unit,
    onClearClick: () -> Unit,
    onRefreshClick: () -> Unit,
    selectedRequest: MutableState<SelectedRequest?>,
) {
    Surface {
        if (screenState.loading && screenState.requests == null) {
            TvLoading()
        } else if (screenState.error != null) {
            TvErrorWithRetry(
                text = stringResource(R.string.error_connection),
                onRetry = onRetry,
            )
        } else if (screenState.requests != null && screenState.user != null) {
            val animatedBackgroundColor by animateColorAsState(
                if (screenState.user.requestsPaused)
                    colorResource(R.color.suspended)
                else
                    MaterialTheme.colorScheme.surface,
                label = "color"
            )

            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize().drawBehind { drawRect(animatedBackgroundColor) },
                state = rememberLazyGridState(),
                columns = GridCells.Fixed(gridColumnCount),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 80.dp, end = 40.dp, top = 20.dp, bottom = 20.dp),
            ) {
                item(
                    span = { GridItemSpan(gridColumnCount) },
                    key = "actions",
                    contentType = "actions"
                ) {
                    ActionsRow(
                        modifier = Modifier.padding(bottom = 16.dp),
                        user = screenState.user,
                        onResumeClick = onResumeClick,
                        onSuspendClick = onSuspendClick,
                        onRequestFavesClick = onRequestFavesClick,
                        onRequestUnratedClick = onRequestUnratedClick,
                        onClearClick = onClearClick,
                        onRefreshClick = onRefreshClick,
                    )
                }
                itemsIndexed(
                    items = screenState.requests,
                    key = { _, item -> item.songId },
                    contentType = { _, _ -> "request" }
                ) { i, item ->
                    TvRequestSongCard(
                        modifier = Modifier.animateItem()
                            .saveLastFocused("request_${item.songId}"),
                        item = item,
                        onClick = { selectedRequest.value = SelectedRequest(item, i) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsRow(
    modifier: Modifier = Modifier,
    user: UserState,
    onResumeClick: () -> Unit,
    onSuspendClick: () -> Unit,
    onRequestFavesClick: () -> Unit,
    onRequestUnratedClick: () -> Unit,
    onClearClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        key("suspend_resume_btn") {
            SuspendResumeButton(
                modifier = Modifier.saveLastFocused("suspend_resume_btn"),
                user = user,
                onResumeClick = onResumeClick,
                onSuspendClick = onSuspendClick,
            )
        }
        key("request_faves_btn") {
            RequestActionButton(
                modifier = Modifier.saveLastFocused("request_faves_btn"),
                onClick = onRequestFavesClick,
                painter = painterResource(R.drawable.ic_request_faves_24dp),
                text = stringResource(R.string.action_request_faves),
            )
        }
        key("request_unrated_btn") {
            RequestActionButton(
                modifier = Modifier.saveLastFocused("request_unrated_btn"),
                onClick = onRequestUnratedClick,
                painter = painterResource(R.drawable.ic_request_unrated_24dp),
                text = stringResource(R.string.action_request_unrated),
            )
        }
        key("clear_btn") {
            RequestActionButton(
                modifier = Modifier.saveLastFocused("clear_btn"),
                onClick = onClearClick,
                painter = painterResource(R.drawable.ic_request_clear_24dp),
                text = stringResource(R.string.action_clear_requests),
            )
        }
        key("refresh_btn") {
            RequestActionButton(
                modifier = Modifier.saveLastFocused("refresh_btn"),
                onClick = onRefreshClick,
                painter = painterResource(R.drawable.ic_refresh_white_24dp),
                text = stringResource(R.string.action_refresh),
            )
        }
    }
}

@Composable
private fun SuspendResumeButton(
    modifier: Modifier = Modifier,
    user: UserState,
    onResumeClick: () -> Unit,
    onSuspendClick: () -> Unit,
) {
    val onClick = if (user.requestsPaused) {
        onResumeClick
    } else {
        onSuspendClick
    }
    val painter = if (user.requestsPaused) {
        painterResource(R.drawable.ic_request_resume_24dp)
    } else {
        painterResource(R.drawable.ic_request_suspend_24dp)
    }
    val text = if (user.requestsPaused) {
        stringResource(R.string.action_resume)
    } else {
        stringResource(R.string.action_suspend)
    }
    RequestActionButton(
        modifier = modifier,
        onClick = onClick,
        painter = painter,
        text = text,
    )
}

@Composable
private fun RequestActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    painter: Painter,
    text: String,
) {
    Button(modifier = modifier, onClick = onClick) {
        Icon(painter = painter, contentDescription = text)
        Spacer(Modifier.size(6.dp))
        Text(text = text)
    }
}

private class TvRequestsScreenPreviewParameter : PreviewParameterProvider<RequestsScreenState> {
    override val values: Sequence<RequestsScreenState> = sequenceOf(
        RequestsScreenState(
            requests = requestStateData.toMutableStateList(),
            user = userStateData[0],
        ),
        RequestsScreenState(
            requests = mutableStateListOf(),
            user = userStateData[1],
        ),
        RequestsScreenState(
            error = OperationError(OperationError.Server),
        ),
    )
}

@PreviewTv
@Composable
private fun TvRequestsScreenPreview(@PreviewParameter(TvRequestsScreenPreviewParameter::class) screenState: RequestsScreenState) {
    PreviewTvTheme {
        TvRequestsScreen(
            screenState = screenState,
            onRetry = {},
            onResumeClick = {},
            onSuspendClick = {},
            onRequestFavesClick = {},
            onRequestUnratedClick = {},
            onClearClick = {},
            onRefreshClick = {},
            selectedRequest = remember { mutableStateOf(null) },
        )
    }
}

data class SelectedRequest(
    val request: RequestState,
    val index: Int,
)

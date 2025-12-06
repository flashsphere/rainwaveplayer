package com.flashsphere.rainwaveplayer.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.MenuIcon
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.ToastHandler
import com.flashsphere.rainwaveplayer.ui.animateScrollToItem
import com.flashsphere.rainwaveplayer.ui.animation.fadeIn
import com.flashsphere.rainwaveplayer.ui.animation.fadeOut
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarActions
import com.flashsphere.rainwaveplayer.ui.appbar.toAppBarAction
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.enterAlwaysScrollBehavior
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.item.SwipeToDismissBackground
import com.flashsphere.rainwaveplayer.ui.navigation.Library
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlaying
import com.flashsphere.rainwaveplayer.ui.navigation.Search
import com.flashsphere.rainwaveplayer.ui.rememberNoFlingSwipeToDismissBoxState
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.RequestsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import com.flashsphere.rainwaveplayer.view.viewmodel.RequestsScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

var requestsLongPressHintShown = false

@Composable
fun RequestsScreen(
    navigator: Navigator,
    viewModel: RequestsScreenViewModel,
    stationFlow: StateFlow<Station?>,
    onMenuClick: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return

    val context = LocalContext.current

    val toastState = remember { mutableStateOf<Toast?>(null) }

    val requestsScreenState = viewModel.requestsScreenState.collectAsStateWithLifecycle().value

    LifecycleStartEffect(station) {
        viewModel.subscribeStationInfo(station, false)

        onStopOrDispose {
            viewModel.unsubscribeStationInfo()
        }
    }

    RequestsScreen(
        navigator = navigator,
        station = station,
        requestsScreenState = requestsScreenState,
        events = viewModel.snackbarEvents,
        onRefresh = { viewModel.subscribeStationInfo(true) },
        onClearClick = viewModel::clearRequests,
        onDelete = viewModel::deleteRequest,
        onReorderItem = viewModel::reorderRequestItem,
        onReorder = viewModel::reorderRequests,
        onReorderItemToTop = { request, index ->
            if (index != 0) {
                viewModel.reorderItemToTop(request, index)
                requestsLongPressHintShown = true
                toastState.value?.cancel()
            }
        },
        onRequestUnratedClick = viewModel::requestUnrated,
        onRequestFavesClick = viewModel::requestFavorites,
        onResumeClick = viewModel::resumeQueue,
        onSuspendClick = viewModel::suspendQueue,
        onRequestClick = {
            if (!requestsLongPressHintShown) {
                requestsLongPressHintShown = true
                toastState.value = Toast.makeText(context, R.string.requests_long_press_hint,
                    Toast.LENGTH_LONG)
            }
        },
        onMenuClick = onMenuClick,
        scrollToTop = scrollToTop,
    )

    ToastHandler(state = toastState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestsScreen(
    navigator: Navigator,
    station: Station,
    requestsScreenState: RequestsScreenState,
    events: Flow<SnackbarEvent>,
    onRefresh: () -> Unit,
    onClearClick: () -> Unit,
    onDelete: (request: RequestState, index: Int) -> Boolean,
    onReorderItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorder: () -> Unit,
    onReorderItemToTop: (request: RequestState, index: Int) -> Unit,
    onRequestUnratedClick: () -> Unit,
    onRequestFavesClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSuspendClick: () -> Unit,
    onRequestClick: () -> Unit,
    onMenuClick: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    val listItemPadding = LocalUiScreenConfig.current.listItemPadding

    val scope  = rememberCoroutineScope()

    val showActionsMenu = rememberSaveable { mutableStateOf(false) }

    val requests = requestsScreenState.requests

    val scrollBehavior = enterAlwaysScrollBehavior(
        scrollToTop = scrollToTop,
        canScroll = { !requests.isNullOrEmpty() && !showActionsMenu.value }
    )

    LaunchedEffect(requests) {
        if (requests.isNullOrEmpty()) {
            scrollBehavior.state.heightOffset = 0F
        }
    }

    AppScaffold(
        appBarContent = {
            AppBarTitle(
                title = stringResource(id = R.string.action_requests),
                subtitle = station.name,
                onClick = { scope.launch { scrollToTop.value = true } },
            )
        },
        appBarScrollBehavior = scrollBehavior,
        navigationIcon = {
            if (LocalUiSettings.current.navigationSuiteType == NavigationSuiteType.None) {
                BackIcon(onBackClick = { navigator.goBack() })
            } else {
                MenuIcon(onMenuClick = onMenuClick)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showActionsMenu.value = !showActionsMenu.value },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                Text(text = stringResource(id = R.string.actions))
            }
        },
        appBarActions = {
            if (LocalUiSettings.current.navigationSuiteType == NavigationSuiteType.None) {
                AppBarActions(toAppBarAction(navigator,
                    listOf(NowPlaying, Library, Search)))
            }
        },
        snackbarEvents = events,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = requestsScreenState.user?.requestsPaused == true) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalUiScreenConfig.current.listItemLineHeight)
                    .background(color = colorResource(id = R.color.suspended))
                    .padding(start = listItemPadding, end = listItemPadding),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.requests_suspended),
                        fontWeight = FontWeight.W500,
                        style = AppTypography.bodyMedium,
                        modifier = Modifier.weight(1F))
                    TextButton(onClick = onResumeClick,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.textButtonColors()) {
                        Text(text = stringResource(id = R.string.btn_resume),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.W500,
                            style = AppTypography.bodyMedium)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = requestsScreenState.loading,
                    onRefresh = onRefresh,
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (requests != null) {
                        RequestsList(requestsScreenState = requestsScreenState, onDelete = onDelete,
                            onReorderItem = onReorderItem, onReorder = onReorder,
                            onReorderItemToTop = onReorderItemToTop, onRequestClick = onRequestClick,
                            scrollToTop = scrollToTop)

                        if (!requestsScreenState.loading && requests.isEmpty()) {
                            Text(text = stringResource(id = R.string.no_requests),
                                style = AppTypography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 20.dp,
                                    start = listItemPadding, end = listItemPadding))
                        }
                    }
                }

                RequestsError(
                    requestsScreenState = requestsScreenState,
                    snackbarHostState = snackbarHostState,
                    onRetry = onRefresh
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            ActionsMenu(visible = showActionsMenu,
                snackbarHostState = snackbarHostState,
                userState = requestsScreenState.user,
                onHide = { showActionsMenu.value = false},
                onClearClick = onClearClick,
                onRequestUnratedClick = onRequestUnratedClick,
                onRequestFavesClick = onRequestFavesClick,
                onResumeClick = onResumeClick,
                onPauseClick = onSuspendClick)
        }
    }
}

@Composable
private fun RequestsList(
    requestsScreenState: RequestsScreenState,
    onReorderItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorder: () -> Unit,
    onReorderItemToTop: (request: RequestState, index: Int) -> Unit,
    onDelete: (request: RequestState, index: Int) -> Boolean,
    onRequestClick: () -> Unit, scrollToTop: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()
    val reorderableLazyListState = rememberReorderableLazyGridState(gridState) { from, to ->
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onReorderItem(from.index, to.index)
    }

    LaunchedEffect(scrollToTop) {
        snapshotFlow { scrollToTop.value }
            .filter { it }
            .collect {
                scrollToTop.value = false
                animateScrollToItem(scope, gridState, 0)
            }
    }

    LazyVerticalGrid(state = gridState,
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(LocalUiScreenConfig.current.gridSpan),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        requestsScreenState.requests?.let {
            itemsIndexed(items = it, key = { _, item -> item.songId }) { i, item ->
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = item.songId,
                ) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "ReorderRequests")
                    Surface(shadowElevation = elevation) {
                        RequestItem(scope = this, request = item, index = i,
                            onDelete = onDelete, onReorder = onReorder,
                            onReorderItemToTop = { request, index ->
                                onReorderItemToTop(request, index)
                                animateScrollToItem(scope, gridState, 0)
                            },
                            onClick = onRequestClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestItem(
    scope: ReorderableCollectionItemScope,
    request: RequestState,
    index: Int,
    onDelete: (request: RequestState, index: Int) -> Boolean,
    onReorder: () -> Unit,
    onReorderItemToTop: (request: RequestState, index: Int) -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val currentItem by rememberUpdatedState(request)
    val currentIndex by rememberUpdatedState(index)

    val dismissState = rememberNoFlingSwipeToDismissBoxState(
        confirmValueChange = { state ->
            if (state != SwipeToDismissBoxValue.Settled) {
                onDelete(currentItem, currentIndex)
            } else {
                false
            }
        },
        positionalThreshold = { it * .2F }
    )

    val cooldownText = request.getCooldownText(context)
    val bgColor = if (cooldownText != null) {
        Modifier.background(colorResource(id = R.color.cooldown_background))
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface)
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeToDismissBackground(dismissState) },
        onDismiss = {
            if (it != SwipeToDismissBoxValue.Settled) {
                onDelete(currentItem, currentIndex)
            }
        },
    ) {
        Row(modifier = Modifier.then(bgColor).fillMaxWidth()
            .combinedClickable(
                interactionSource = null,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReorderItemToTop(currentItem, currentIndex)
                },
                onLongClickLabel = stringResource(R.string.action_move_to_top)
            )
            .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
        ) {
            AlbumArt(
                art = request.art,
                modifier = with(scope) {
                    Modifier
                        .size(LocalUiScreenConfig.current.nowPlayingImageSize)
                        .draggableHandle(onDragStarted = {
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        }, onDragStopped = {
                            onReorder()
                        })
                }
            )
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)) {
                Text(text = request.title, style = AppTypography.bodyLarge,
                    lineHeight = LocalUiScreenConfig.current.songItemTitleLineHeight)
                Text(text = request.albumName, style = AppTypography.bodyMedium,
                    lineHeight = LocalUiScreenConfig.current.songItemAlbumLineHeight,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp))

                cooldownText?.let {
                    Text(text = it, style = AppTypography.bodyMedium,
                        lineHeight = LocalUiScreenConfig.current.songItemCooldownLineHeight,
                        color = colorResource(id = R.color.cooldown_text),
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionsMenu(
    visible: MutableState<Boolean>,
    userState: UserState?,
    snackbarHostState: SnackbarHostState,
    onHide: () -> Unit,
    onClearClick: () -> Unit,
    onRequestUnratedClick: () -> Unit,
    onRequestFavesClick: () -> Unit,
    onResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(visible.value) {
        if (visible.value) {
            scope.launch {
                scrollState.scrollTo(scrollState.maxValue)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    BackHandler(visible.value) {
        visible.value = false
    }

    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
                .clickable(interactionSource = null, indication = null, onClick = onHide)
                .background(color = colorResource(id = R.color.requests_fab_container_bg))) {

            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .wrapContentSize()
                    .padding(end = 16.dp, bottom = 80.dp)) {
                Action(
                    action = stringResource(id = R.string.action_clear_requests),
                    actionIcon = R.drawable.ic_request_clear_24dp) {
                    onHide()
                    onClearClick()
                }
                Action(
                    action = stringResource(id = R.string.action_request_unrated),
                    actionIcon = R.drawable.ic_request_unrated_24dp) {
                    onHide()
                    onRequestUnratedClick()
                }
                Action(
                    action = stringResource(id = R.string.action_request_faves),
                    actionIcon = R.drawable.ic_request_faves_24dp) {
                    onHide()
                    onRequestFavesClick()
                }
                if (userState != null) {
                    if (userState.requestsPaused) {
                        Action(
                            action = stringResource(id = R.string.action_resume),
                            actionIcon = R.drawable.ic_request_resume_24dp) {
                            onHide()
                            onResumeClick()
                        }
                    } else {
                        Action(
                            action = stringResource(id = R.string.action_suspend),
                            actionIcon = R.drawable.ic_request_suspend_24dp) {
                            onHide()
                            onPauseClick()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Action(
    modifier: Modifier = Modifier,
    action: String, @DrawableRes actionIcon: Int,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(bottom = 16.dp)
            .focusProperties { canFocus = false }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Text(text = action,
            style = AppTypography.labelLarge,
            modifier = Modifier
                .padding(end = 8.dp)
                .focusProperties { canFocus = false }
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(8.dp)
        )

        SmallFloatingActionButton(onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            interactionSource = interactionSource,
        ) {
            Icon(painter = painterResource(id = actionIcon),
                contentDescription = action)
        }
    }
}

@Composable
private fun RequestsError(requestsScreenState: RequestsScreenState,
                          snackbarHostState: SnackbarHostState,
                          onRetry: () -> Unit) {
    if (requestsScreenState.error == null) return
    AppError(
        showAsSnackbar = requestsScreenState.requests != null,
        snackbarHostState = snackbarHostState,
        error = requestsScreenState.error,
        onRetry = onRetry
    )
}

private class RequestsPreviewParameter : PreviewParameterProvider<RequestsScreenState> {
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

@Preview
@Composable
private fun RequestsScreenPreview(
    @PreviewParameter(RequestsPreviewParameter::class) requestsScreenState: RequestsScreenState
) {
    PreviewTheme {
        RequestsScreen(
            navigator = rememberPreviewNavigator(),
            station = stations[0],
            requestsScreenState = requestsScreenState,
            events = remember { MutableSharedFlow() },
            onRefresh = {},
            onClearClick = {},
            onDelete = { _, _ -> true },
            onReorderItem = { _, _ -> },
            onReorder = {},
            onReorderItemToTop = { _, _ -> },
            onRequestUnratedClick = {},
            onRequestFavesClick = {},
            onResumeClick = {},
            onSuspendClick = {},
            onRequestClick = {},
            onMenuClick = {},
            scrollToTop = remember { mutableStateOf(false) },
        )
    }
}

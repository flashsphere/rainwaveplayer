package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.item.tv.TvListItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvRequestSong
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.requestStateData
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState
import com.flashsphere.rainwaveplayer.view.viewmodel.RequestsScreenViewModel

@Composable
fun TvRequestActionsScreen(
    viewModel: RequestsScreenViewModel,
    selectedRequest: RequestState,
    onDismissRequest: () -> Unit,
) {
    BackHandler {
        onDismissRequest()
    }

    val screenState = viewModel.requestsScreenState.collectAsStateWithLifecycle().value
    if (screenState.requests == null) {
        TvLoading()
        return
    }

    val found = remember { mutableStateOf(false) }
    LaunchedEffect(screenState.requests) {
        snapshotFlow { screenState.requests.toList() }.collect { requests ->
            found.value = requests.any { selectedRequest.songId == it.songId }
            if (!found.value) {
                onDismissRequest()
            }
        }
    }
    if (!found.value) {
        return
    }

    TvRequestActionsScreen(
        requests = screenState.requests,
        item = selectedRequest,
        onMoveToTop = { item, index ->
            viewModel.reorderItemToTop(item, index)
        },
        onMove = { fromIndex, toIndex ->
            viewModel.reorderRequestItem(fromIndex, toIndex)
            viewModel.reorderRequests()
        },
        onDelete = { item, index -> viewModel.deleteRequest(item, index) },
    )
}

@Composable
fun TvRequestActionsScreen(
    requests: List<RequestState>,
    item: RequestState,
    onMoveToTop: (item: RequestState, index: Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDelete: (item: RequestState, index: Int) -> Unit,
) {
    val index = requests.indexOfFirst { it.songId == item.songId }
    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(start = 80.dp, end = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RequestList(modifier = Modifier.weight(0.7F), requests = requests, index = index)
            RequestActions(
                modifier = Modifier.weight(0.3F),
                listSize = requests.size,
                index = index,
                onMoveToTop = { onMoveToTop(item, index) },
                onMoveUp = {
                    val toIndex = index - 1
                    if (index > 0) {
                        onMove(index, toIndex)
                    }
                },
                onMoveDown = {
                    val toIndex = index + 1
                    if (toIndex < requests.size) {
                        onMove(index, toIndex)
                    }
                },
                onDelete = { onDelete(item, index) },
            )
        }
    }
}

@Composable
private fun RequestList(
    modifier: Modifier,
    requests: List<RequestState>,
    index: Int,
) {
    val itemWidth = LocalUiScreenConfig.current.tvCardWidth.times(1.5F)
    val itemImageSize = LocalUiScreenConfig.current.tvCardWidth / 2
    val listVerticalPadding = (LocalUiScreenConfig.current.windowSize.height / 2) - (itemImageSize / 2)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = index)

    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
        contentPadding = PaddingValues(vertical = listVerticalPadding),
    ) {
        itemsIndexed(
            items = requests,
            key = { _, item -> item.songId }
        ) { i, item ->
            val border = if (i == index) {
                Border(
                    border = BorderStroke(width = 3.dp, color = MaterialTheme.colorScheme.border),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Border.None
            }
            Surface(
                modifier = Modifier.animateItem(),
                shape = RoundedCornerShape(8.dp),
                border = border,
            ) {
                TvRequestSong(
                    modifier = Modifier.width(itemWidth),
                    item = item,
                    imageSize = itemImageSize
                )
            }
        }
    }
}

@Composable
private fun RequestActions(
    modifier: Modifier,
    listSize: Int,
    index: Int,
    onMoveToTop: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val hasFocus = remember { mutableStateOf(false) }
    val focusProperties: FocusProperties.() -> Unit = remember { {
        left = FocusRequester.Cancel
        right = FocusRequester.Cancel
        previous = FocusRequester.Cancel
        next = FocusRequester.Cancel
    } }

    LazyColumn(
        modifier = modifier.focusRequester(focusRequester)
            .onFocusChanged { hasFocus.value = it.hasFocus }
            .onGloballyPositioned {
                if (!hasFocus.value) {
                    focusRequester.requestFocus()
                }
            },
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (index > 0) {
            item(key = "move_to_top") {
                TvListItem(
                    modifier = Modifier.animateItem()
                        .focusProperties(focusProperties),
                    onClick = onMoveToTop,
                    text = stringResource(R.string.action_move_to_top),
                )
            }
            item(key = "move_up") {
                TvListItem(
                    modifier = Modifier.animateItem()
                        .focusProperties(focusProperties),
                    onClick = onMoveUp,
                    text = stringResource(R.string.action_move_up),
                )
            }
        }
        if (index < listSize - 1) {
            item(key = "move_down") {
                TvListItem(
                    modifier = Modifier.animateItem()
                        .focusProperties(focusProperties),
                    onClick = onMoveDown,
                    text = stringResource(R.string.action_move_down),
                )
            }
        }
        item(key = "delete") {
            TvListItem(
                modifier = Modifier.animateItem()
                    .focusProperties(focusProperties),
                onClick = onDelete,
                text = stringResource(R.string.action_delete),
            )
        }
    }
}

@PreviewTv
@Composable
private fun TvRequestActionsScreenPreview() {
    val requests = remember { requestStateData.toMutableStateList() }
    PreviewTvTheme {
        TvRequestActionsScreen(
            requests = requests,
            item = requests[0],
            onMoveToTop = { _, _ -> },
            onMove = { _, _ -> },
            onDelete = { _, _ -> },
        )
    }
}

package com.flashsphere.rainwaveplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxState.Companion.Saver
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LeftToRightLayout(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        content()
    }
}

fun itemSpan(
    gridColumnCount: Int
): (LazyGridItemSpanScope.() -> GridItemSpan)? {
    return if (gridColumnCount == 1) {
        null
    } else {
        { GridItemSpan(gridColumnCount) }
    }
}

fun <T> itemsSpan(
    gridColumnCount: Int,
    block: LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan
): (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? {
    return if (gridColumnCount == 1) {
        null
    } else {
        block
    }
}

fun <T> itemsSpan(
    gridColumnCount: Int,
    block: LazyGridItemSpanScope.(item: T) -> GridItemSpan
): (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? {
    return if (gridColumnCount == 1) {
        null
    } else {
        block
    }
}

@Composable
fun rememberNoFlingSwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
    confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { true },
    positionalThreshold: (totalDistance: Float) -> Float = SwipeToDismissBoxDefaults.positionalThreshold,
): SwipeToDismissBoxState {
    val density = Density(Float.POSITIVE_INFINITY)
    return rememberSaveable(
        saver =
            Saver(
                confirmValueChange = confirmValueChange,
                density = density,
                positionalThreshold = positionalThreshold
            )
    ) {
        SwipeToDismissBoxState(initialValue, density, confirmValueChange, positionalThreshold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshBox(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
            threshold = 60.dp
        )
    },
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.pullToRefresh(
            enabled = enabled,
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ),
        contentAlignment = contentAlignment
    ) {
        content()
        indicator()
    }
}

fun animateScrollToItem(scope: CoroutineScope, state: LazyGridState, index: Int) {
    scope.launch(CoroutineName("Animate scroll to item")) {
        suspendRunCatching { state.animateScrollToItem(index) }
    }
}

fun scrollToItem(scope: CoroutineScope, state: LazyGridState, index: Int) {
    scope.launch(CoroutineName("Scroll to item")) {
        suspendRunCatching { state.scrollToItem(index) }
    }
}

package com.flashsphere.rainwaveplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarAction
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarActions
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    appBarContent: @Composable () -> Unit = {},
    appBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
    appBarActions: @Composable RowScope.() -> Unit = {},
    snackbarEvents: Flow<SnackbarEvent> = emptyFlow(),
    content: @Composable AppScaffoldScope.() -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets.ime,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(appBarScrollBehavior.nestedScrollConnection),
        snackbarHost = {
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState()

            LaunchedEffect(swipeToDismissBoxState.currentValue) {
                if (swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
                    swipeToDismissBoxState.reset()
                }
            }

            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                backgroundContent = {},
                content = { SnackbarHost(hostState = snackbarHostState) },
                onDismiss = {
                    if (it != SwipeToDismissBoxValue.Settled) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                },
            )
        },
        topBar = {
            TopAppBar(
                title = appBarContent,
                navigationIcon = navigationIcon,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                scrollBehavior = appBarScrollBehavior,
                actions = {
                    appBarActions()
                },
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top),
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()
        ) {
            val scope = remember(snackbarHostState) { AppScaffoldScopeImpl(snackbarHostState) }
            scope.content()

            EventsSnackbar(snackbarHostState, snackbarEvents)
        }
    }
}

@Stable
interface AppScaffoldScope {
    val snackbarHostState: SnackbarHostState
}

@Stable
internal class AppScaffoldScopeImpl(
    override val snackbarHostState: SnackbarHostState
) : AppScaffoldScope

@Composable
fun AppError(error: OperationError?, onRetry: () -> Unit) {
    if (error == null) return

    val context = LocalContext.current
    val message = error.getMessage(context, stringResource(R.string.error_connection))
    ErrorWithRetry(
        text = message,
        onRetry = onRetry,
    )
}

@Composable
fun AppError(
    showAsSnackbar: Boolean,
    snackbarHostState: SnackbarHostState,
    error: OperationError,
    onRetry: () -> Unit
) {
    if (showAsSnackbar) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        LaunchedEffect(error) {
            val data = error.toSnackbarData(context, onRetry)
            scope.launch {
                launchSnackbar(snackbarHostState, data)
            }
        }
    } else {
        AppError(error = error, onRetry = onRetry)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun enterAlwaysScrollBehavior(scrollToTop: MutableState<Boolean>, canScroll: () -> Boolean = { true }): TopAppBarScrollBehavior {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = canScroll)
    LaunchedEffect(scrollToTop) {
        snapshotFlow { scrollToTop.value }
            .filter { it }
            .collect { scrollBehavior.state.heightOffset = 0F }
    }
    return scrollBehavior
}

@Composable
fun MenuIcon(onMenuClick: () -> Unit) {
    Tooltip(stringResource(id = R.string.nav_menu)) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(id = R.string.nav_menu)
            )
        }
    }
}

@Composable
fun BackIcon(onBackClick: () -> Unit) {
    Tooltip(stringResource(id = R.string.nav_back)) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.nav_back)
            )
        }
    }
}

@Composable
fun CloseIcon(onCloseClick: () -> Unit) {
    Tooltip(stringResource(id = R.string.nav_close)) {
        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.nav_close)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ScaffoldWithAppBarPreview() {
    val activated = remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState()
    PreviewTheme {
        AppScaffold(
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(id = R.string.nav_menu)
                    )
                }
            },
            appBarContent = {
                if (activated.value) {
                    SearchTextField(
                        state = textFieldState,
                        onSubmit = {},
                        label = {
                            SearchTextFieldLabel(
                                painter = rememberVectorPainter(Icons.Filled.Search),
                                text = "Search"
                            )
                        }
                    )
                } else {
                    AppBarTitle(title = "Some title", subtitle = "Some subtitle")
                }
            },
            appBarActions = {
                AppBarActions(listOf(
                    AppBarAction(
                        icon = rememberVectorPainter(Icons.Filled.FilterList),
                        text = "Filter",
                        onClick = { activated.value = true },
                    ),
                ), listOf(
                    AppBarAction(
                        icon = rememberVectorPainter(Icons.Filled.FilterList),
                        text = "Filter",
                        onClick = { activated.value = true },
                    )
                ))
            },
            content = {
                Text(text = "test", modifier = Modifier.fillMaxSize())
            }
        )
    }
}

@Preview
@Composable
private fun AppErrorPreview() {
    PreviewTheme {
        Surface {
            AppError(error = OperationError(OperationError.Server)) {}
        }
    }
}

package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

@Composable
fun NavigationSuiteScaffold(
    contentWindowInsets: WindowInsets = NoWindowInsets,
    layoutType: NavigationSuiteType,
    navigationSuite: @Composable () -> Unit,
    content: @Composable () -> Unit = {},
) {
    NavigationSuiteScaffoldLayout(
        contentWindowInsets = contentWindowInsets,
        layoutType = layoutType,
        navigationSuite = navigationSuite,
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            content()
        }
    }
}

@Composable
fun NavigationSuiteScaffoldLayout(
    contentWindowInsets: WindowInsets,
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType,
    content: @Composable (PaddingValues) -> Unit = {},
) {
    val isNavigationBar = layoutType == NavigationSuiteType.NavigationBar

    if (isNavigationBar) {
        SubcomposeLayout {constraints ->
            val layoutWidth = constraints.maxWidth
            val layoutHeight = constraints.maxHeight

            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

            val navigationSuitePlaceable = subcompose(NavigationSuiteLayoutIdTag) { navigationSuite() }
                .fastMap { it.measure(looseConstraints) }
            val navigationSuiteHeight = navigationSuitePlaceable.fastMaxBy { it.height }?.height

            val bodyContentPlaceable = subcompose(ContentLayoutIdTag) {
                val insets = contentWindowInsets.asPaddingValues(this@SubcomposeLayout)
                val innerPadding = PaddingValues(
                    top = insets.calculateTopPadding(),
                    bottom = if (navigationSuitePlaceable.isEmpty() || navigationSuiteHeight == null) {
                        insets.calculateBottomPadding()
                    } else {
                        navigationSuiteHeight.toDp()
                    },
                    start = insets.calculateStartPadding((this@SubcomposeLayout).layoutDirection),
                    end = insets.calculateEndPadding((this@SubcomposeLayout).layoutDirection)
                )
                content(innerPadding)
            }.fastMap { it.measure(looseConstraints) }

            layout(layoutWidth, layoutHeight) {
                bodyContentPlaceable.fastForEach { it.place(0, 0) }
                navigationSuitePlaceable.fastForEach { it.place(0, layoutHeight - (navigationSuiteHeight ?: 0)) }
            }
        }
    } else {
        SubcomposeLayout {constraints ->
            val layoutWidth = constraints.maxWidth
            val layoutHeight = constraints.maxHeight

            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

            val navigationSuitePlaceable = subcompose(NavigationSuiteLayoutIdTag) { navigationSuite() }
                .fastMap { it.measure(looseConstraints) }
            val navigationSuiteWidth = navigationSuitePlaceable.fastMaxBy { it.width }?.width

            val bodyContentPlaceable = subcompose(ContentLayoutIdTag) {
                val insets = contentWindowInsets.asPaddingValues(this@SubcomposeLayout)
                val innerPadding = PaddingValues(
                    top = insets.calculateTopPadding(),
                    bottom = insets.calculateBottomPadding(),
                    start = if (navigationSuitePlaceable.isEmpty() || navigationSuiteWidth == null) {
                        insets.calculateStartPadding((this@SubcomposeLayout).layoutDirection)
                    } else {
                        navigationSuiteWidth.toDp()
                    },
                    end = insets.calculateEndPadding((this@SubcomposeLayout).layoutDirection)
                )
                content(innerPadding)
            }.fastMap { it.measure(looseConstraints) }

            layout(layoutWidth, layoutHeight) {
                bodyContentPlaceable.fastForEach { it.place(0, 0) }
                navigationSuitePlaceable.fastForEach { it.place(0, 0) }
            }
        }
    }
}

private val NoWindowInsets = WindowInsets(0, 0, 0, 0)

private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val ContentLayoutIdTag = "content"

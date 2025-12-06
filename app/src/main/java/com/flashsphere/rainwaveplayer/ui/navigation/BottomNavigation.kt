package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTablet
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme

@Composable
fun BottomNavigation(
    navigator: Navigator,
    alwaysShowLabel: Boolean,
    scrollToTop: MutableState<Boolean>,
) {
    NavigationBar {
        Routes(
            navigator = navigator,
            scrollToTop = scrollToTop,
        ) { route, selected, onClick ->
            BottomNavigationItem(
                route = route,
                alwaysShowLabel = alwaysShowLabel,
                selected = selected,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItem(
    route: TopLevelRoute,
    alwaysShowLabel: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(route.title)
    NavigationBarItem(
        icon = {
            Tooltip(title) {
                Icon(painter = painterResource(route.icon), contentDescription = title)
            }
        },
        label = {
            Text(text = title, textAlign = TextAlign.Center, maxLines = 1, overflow = Ellipsis)
        },
        alwaysShowLabel = alwaysShowLabel,
        selected = selected,
        onClick = onClick,
    )
}

@Preview
@PreviewTablet
@Composable
private fun NavigationBarWithLabelPreview() {
    var selectedItem by remember { mutableIntStateOf(0) }
    PreviewTheme {
        Surface {
            NavigationBar {
                topLevelRoutes.forEachIndexed { index, item ->
                    BottomNavigationItem(
                        route = item,
                        alwaysShowLabel = true,
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    }
}

@Preview
@PreviewTablet
@Composable
private fun NavigationBarWithoutLabelPreview() {
    var selectedItem by remember { mutableIntStateOf(0) }
    PreviewTheme {
        Surface {
            NavigationBar {
                topLevelRoutes.forEachIndexed { index, item ->
                    BottomNavigationItem(
                        route = item,
                        alwaysShowLabel = false,
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    }
}

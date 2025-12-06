package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.item.UserAvatar
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTablet
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.rememberPreviewNavigator
import com.flashsphere.rainwaveplayer.ui.screen.userStateData
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SideNavigation(
    navigator: Navigator,
    alwaysShowLabel: Boolean,
    scrollToTop: MutableState<Boolean>,
    userFlow: StateFlow<UserState?>,
    onSleepTimerClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    NavigationRail(
        header = {
            val user = userFlow.collectAsStateWithLifecycle().value
            UserAvatar(modifier = Modifier.padding(top = 4.dp), user = user, size = 40.dp)
        },
    ) {
        Spacer(Modifier.weight(1F))
        Routes(
            navigator = navigator,
            scrollToTop = scrollToTop,
        ) { route, selected, onClick ->
            SideNavigationItem(
                route = route,
                alwaysShowLabel = alwaysShowLabel,
                selected = selected,
                onClick = onClick
            )
        }
        Spacer(Modifier.weight(1F))

        NavigationRailItem(
            icon = {
                Tooltip(stringResource(R.string.sleep_timer)) {
                    Icon(imageVector = Icons.Filled.Snooze, contentDescription = stringResource(R.string.sleep_timer))
                }
            },
            label = { Text(stringResource(R.string.sleep_timer), textAlign = TextAlign.Center) },
            selected = false,
            alwaysShowLabel = alwaysShowLabel,
            onClick = onSleepTimerClick,
        )
        NavigationRailItem(
            icon = {
                Tooltip(stringResource(R.string.settings)) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                }
            },
            label = { Text(stringResource(R.string.settings), textAlign = TextAlign.Center) },
            selected = false,
            alwaysShowLabel = alwaysShowLabel,
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun SideNavigationItem(
    route: TopLevelRoute,
    alwaysShowLabel: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(route.title)

    NavigationRailItem(
        icon = {
            Tooltip(title) {
                Icon(painter = painterResource(route.icon), contentDescription = title)
            }
        },
        label = { Text(title, textAlign = TextAlign.Center) },
        alwaysShowLabel = alwaysShowLabel,
        selected = selected,
        onClick = onClick
    )
}

@PreviewTablet
@Composable
private fun SideNavigationWithLabelPreview() {
    PreviewTheme {
        Surface {
            SideNavigation(
                navigator = rememberPreviewNavigator(),
                alwaysShowLabel = true,
                scrollToTop = remember { mutableStateOf(false) },
                userFlow = remember { MutableStateFlow(userStateData[1]) },
                onSleepTimerClick = {},
                onSettingsClick = {},
            )
        }
    }
}

@PreviewTablet
@Composable
private fun SideNavigationWithoutLabelPreview() {
    PreviewTheme {
        Surface {
            SideNavigation(
                navigator = rememberPreviewNavigator(),
                alwaysShowLabel = false,
                scrollToTop = remember { mutableStateOf(false) },
                userFlow = remember { MutableStateFlow(null) },
                onSleepTimerClick = {},
                onSettingsClick = {},
            )
        }
    }
}

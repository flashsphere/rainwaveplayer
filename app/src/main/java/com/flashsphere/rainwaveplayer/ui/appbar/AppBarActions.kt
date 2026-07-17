package com.flashsphere.rainwaveplayer.ui.appbar

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.navigation.TopLevelRoute
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme

@Composable
fun AppBarActions(
    actions: List<AppBarAction> = emptyList(),
    overflowActions: List<AppBarAction> = emptyList(),
) {
    actions.forEach { action ->
        Tooltip(action.text) {
            IconButton(onClick = action.onClick) {
                Icon(painter = action.icon, contentDescription = action.text)
            }
        }
    }

    if (overflowActions.isEmpty()) return

    val showDropDownMenu = remember { mutableStateOf(false) }

    Tooltip(stringResource(id = R.string.action_more)) {
        IconButton(onClick = { showDropDownMenu.value = true }) {
            Icon(painterResource(id = R.drawable.ic_more_vert), stringResource(id = R.string.action_more))
        }
    }

    DropdownMenu(
        expanded = showDropDownMenu.value,
        onDismissRequest = { showDropDownMenu.value = false }
    ) {
        overflowActions.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.text) },
                leadingIcon = {
                    Icon(painter = action.icon, contentDescription = action.text)
                },
                onClick = {
                    action.onClick()
                    showDropDownMenu.value = false
                }
            )
        }
    }
}

data class AppBarAction(
    val icon: Painter,
    val text: String,
    val onClick: () -> Unit,
)

@Composable
fun toAppBarAction(navigator: Navigator, routes: List<TopLevelRoute>): List<AppBarAction> {
    return routes.map { route ->
        key(route) {
            AppBarAction(
                icon = painterResource(route.icon),
                text = stringResource(route.title),
                onClick = { navigator.navigate(route) }
            )
        }
    }
}

@Preview
@Composable
private fun AppBarActionsPreview() {
    PreviewTheme {
        Surface {
            Row {
                AppBarActions(listOf(
                    AppBarAction(
                        icon = painterResource(R.drawable.ic_filter_list),
                        text = "Filter",
                        onClick = {},
                    ),
                    AppBarAction(
                        icon = painterResource(R.drawable.ic_search),
                        text = "Search",
                        onClick = {},
                    ),
                ), listOf(
                    AppBarAction(
                        icon = painterResource(R.drawable.ic_menu),
                        text = "Menu",
                        onClick = {},
                    ),
                    AppBarAction(
                        icon = painterResource(R.drawable.ic_arrow_drop_down),
                        text = "Dropdown",
                        onClick = {},
                    ),
                ))
            }
        }
    }
}

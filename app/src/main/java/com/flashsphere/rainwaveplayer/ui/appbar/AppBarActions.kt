package com.flashsphere.rainwaveplayer.ui.appbar

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.navigation.Route
import com.flashsphere.rainwaveplayer.ui.navigation.navigateToRoute
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
            Icon(Icons.Filled.MoreVert, stringResource(id = R.string.action_more))
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
fun toAppBarAction(navController: NavHostController, routes: List<Route>): List<AppBarAction> {
    return routes.map { route ->
        AppBarAction(
            icon = painterResource(route.icon),
            text = stringResource(route.title),
            onClick = remember(route) { { navController.navigateToRoute(route) } }
        )
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
                        icon = rememberVectorPainter(Icons.Filled.FilterList),
                        text = "Filter",
                        onClick = {},
                    ),
                    AppBarAction(
                        icon = rememberVectorPainter(Icons.Filled.Search),
                        text = "Filter",
                        onClick = {},
                    ),
                ), listOf(
                    AppBarAction(
                        icon = rememberVectorPainter(Icons.Filled.Menu),
                        text = "Filter",
                        onClick = {},
                    ),
                    AppBarAction(
                        icon = rememberVectorPainter(Icons.Filled.ArrowDropDown),
                        text = "Filter",
                        onClick = {},
                    ),
                ))
            }
        }
    }
}

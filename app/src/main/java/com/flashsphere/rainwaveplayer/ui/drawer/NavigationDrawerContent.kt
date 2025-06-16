package com.flashsphere.rainwaveplayer.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.item.HorizontalSeparator
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.stations
import com.flashsphere.rainwaveplayer.ui.screen.userStateData
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.uistate.StationsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawerContent(
    drawerState: DrawerState,
    stationsScreenState: StateFlow<StationsScreenState>,
    stationFlow: StateFlow<Station?>,
    userFlow: StateFlow<UserState?>,
    drawerItemHandler: DrawerItemHandler,
) {
    val scope = rememberCoroutineScope()
    val isLoggedIn = LocalUserCredentials.current.isLoggedIn()

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val width = if (LocalUiScreenConfig.current.windowSize.width <= 300.dp) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.requiredWidth(300.dp)
    }

    ModalDrawerSheet(
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom),
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Start))
            .then(width)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        DrawerHeader(
            userFlow = userFlow,
            onLoginClick = {
                drawerItemHandler.loginClick()
                scope.launch { drawerState.close() }
            }
        )

        Column(modifier = Modifier.fillMaxSize()) {
            DrawerStationItems(
                drawerState = drawerState,
                stationsScreenState = stationsScreenState,
                stationFlow = stationFlow,
                drawerItemHandler = drawerItemHandler,
            )

            DrawerItemSeparator()

            if (isLoggedIn) {
                DrawerItem(
                    drawerState = drawerState,
                    text = stringResource(R.string.all_faves),
                    onClick = drawerItemHandler.allFavesClick
                )
                DrawerItem(
                    drawerState = drawerState,
                    text = stringResource(R.string.recent_votes),
                    onClick = drawerItemHandler.recentVotesClick
                )
                DrawerItem(
                    drawerState = drawerState,
                    text = stringResource(R.string.request_history),
                    onClick = drawerItemHandler.requestHistoryClick
                )

                DrawerItemSeparator()
            }

            DrawerItem(
                drawerState = drawerState,
                text = stringResource(R.string.discord),
                onClick = drawerItemHandler.discordClick
            )
            DrawerItem(
                drawerState = drawerState,
                text = stringResource(R.string.patreon),
                onClick = drawerItemHandler.patreonClick
            )

            DrawerItemSeparator()

            DrawerItem(
                drawerState = drawerState,
                text = stringResource(R.string.sleep_timer),
                onClick = drawerItemHandler.sleepTimerClick
            )
            DrawerItem(
                drawerState = drawerState,
                text = stringResource(R.string.settings),
                onClick = drawerItemHandler.settingsClick
            )
            DrawerItem(
                drawerState = drawerState,
                text = stringResource(R.string.about),
                onClick = drawerItemHandler.aboutClick
            )

            if (isLoggedIn) {
                DrawerItemSeparator()

                DrawerItem(
                    drawerState = drawerState,
                    text = stringResource(R.string.logout),
                    onClick = drawerItemHandler.logoutClick
                )
            }
        }
    }
}

@Composable
private fun DrawerStationItems(
    drawerState: DrawerState,
    stationsScreenState: StateFlow<StationsScreenState>,
    stationFlow: StateFlow<Station?>,
    drawerItemHandler: DrawerItemHandler,
) {
    val stations = stationsScreenState.collectAsStateWithLifecycle().value.stations
    val selectedStation = stationFlow.collectAsStateWithLifecycle().value

    Box(modifier = Modifier.fillMaxWidth()
        .heightIn(min = 48.dp)
        .padding(horizontal = 28.dp),
        contentAlignment = Alignment.CenterStart) {
        Text(text = stringResource(R.string.stations),
            style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    stations?.forEach { station ->
        DrawerItem(
            drawerState = drawerState,
            text = "\t\t" + station.name,
            selected = station.id == selectedStation?.id,
            onClick = { drawerItemHandler.stationClick(station) }
        )
    }
}

@Composable
private fun DrawerItem(
    drawerState: DrawerState,
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val style = if (selected) {
        AppTypography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
    } else {
        AppTypography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    }
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 12.dp),
        selected = selected,
        label = { Text(text = text, style = style) },
        onClick = {
            onClick()
            scope.launch { drawerState.close() }
        }
    )
}

@Composable
private fun DrawerItemSeparator() {
    Box(Modifier.padding(vertical = 8.dp, horizontal = 28.dp)) {
        HorizontalSeparator()
    }
}

private class NavigationDrawerContentPreviewProvider : PreviewParameterProvider<UserCredentials?> {
    override val values: Sequence<UserCredentials?> = sequenceOf(
        UserCredentials(2, ""),
        null,
    )
}

@Preview
@Composable
private fun NavigationDrawerContentPreview(
    @PreviewParameter(NavigationDrawerContentPreviewProvider::class) userCredentials: UserCredentials?
) {
    val userFlow: StateFlow<UserState?> = remember(userCredentials) {
        if (userCredentials != null) {
            MutableStateFlow(userStateData[1])
        } else {
            MutableStateFlow(null)
        }
    }
    PreviewTheme(userCredentials = userCredentials) {
        NavigationDrawerContent(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
            stationsScreenState = MutableStateFlow(StationsScreenState.loaded(stations)),
            stationFlow = MutableStateFlow(stations[0]),
            userFlow = userFlow,
            drawerItemHandler = DrawerItemHandler(
                stationClick = {},
                allFavesClick = {},
                recentVotesClick = {},
                requestHistoryClick = {},
                discordClick = {},
                patreonClick = {},
                sleepTimerClick = {},
                settingsClick = {},
                aboutClick = {},
                loginClick = {},
                logoutClick = {},
            ),
        )
    }
}

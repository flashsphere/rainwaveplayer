package com.flashsphere.rainwaveplayer.ui.drawer

import androidx.compose.runtime.Stable
import com.flashsphere.rainwaveplayer.model.station.Station

@Stable
class DrawerItemHandler(
    val stationClick: (station: Station) -> Unit,
    val allFavesClick: () -> Unit,
    val recentVotesClick: () -> Unit,
    val requestHistoryClick: () -> Unit,
    val discordClick: () -> Unit,
    val patreonClick: () -> Unit,
    val sleepTimerClick: () -> Unit,
    val settingsClick: () -> Unit,
    val aboutClick: () -> Unit,
    val loginClick: () -> Unit,
    val logoutClick: () -> Unit,
)

package com.flashsphere.rainwaveplayer.view.uistate.event

import androidx.compose.runtime.Immutable

interface UiEvent

@Immutable
class RefreshStationInfo(
    val stationId: Int
) : UiEvent

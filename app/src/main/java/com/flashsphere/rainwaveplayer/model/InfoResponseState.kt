package com.flashsphere.rainwaveplayer.model

import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse

class InfoResponseState(
    val loading: Boolean = false,
    val data: InfoResponse? = null,
    val error: Throwable? = null,
)

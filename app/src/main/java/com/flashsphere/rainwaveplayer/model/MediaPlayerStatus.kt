package com.flashsphere.rainwaveplayer.model

import com.flashsphere.rainwaveplayer.model.station.Station

data class MediaPlayerStatus(
    val station: Station,
    val state: State,
) {
    enum class State {
        Playing, Buffering, Stopped
    }

    fun isPlaying(station: Station): Boolean {
        return state != State.Stopped && this.station == station
    }

    fun isStopped(): Boolean = state == State.Stopped
}

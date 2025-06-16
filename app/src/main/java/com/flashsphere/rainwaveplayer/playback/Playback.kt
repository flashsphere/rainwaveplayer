package com.flashsphere.rainwaveplayer.playback

import com.flashsphere.rainwaveplayer.model.station.Station

interface Playback {
    fun play(station: Station)
    fun pause()
    fun stop()
    fun setCallback(callback: Callback)

    interface Callback {
        fun onPlaybackStateChanged(state: Int)
        fun onError(e: Exception)
    }
}

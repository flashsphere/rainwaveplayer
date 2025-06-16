package com.flashsphere.rainwaveplayer.view.activity.delegate

import android.content.Intent
import android.os.Bundle
import com.flashsphere.rainwaveplayer.model.station.Station

interface MainActivityDelegate {
    fun onCreate(savedInstanceState: Bundle?)
    fun onDestroy() {}
    fun onNewIntent(intent: Intent): Boolean = false
    fun onStationsLoaded(stations: List<Station>)
}

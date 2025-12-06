package com.flashsphere.rainwaveplayer.util

import android.os.Bundle
import androidx.navigation3.runtime.NavKey
import com.flashsphere.rainwaveplayer.ui.navigation.DetailRoute
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.EVENT_SCREEN_VIEW
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.SCREEN_CLASS
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.SCREEN_NAME

class AnalyticsOnDestinationChangedListener(
    private val analytics: Analytics,
): Navigator.OnDestinationChangedListener {
    override fun onDestinationChanged(destination: NavKey) {
        val params = Bundle().also {
            val route = destination.javaClass.name
            it.putString(SCREEN_NAME, route.substringAfterLast("."))
            it.putString(SCREEN_CLASS, route)

            if (destination is DetailRoute) {
                it.putInt("id", destination.id)
                it.putString("name", destination.name)
            }
        }
        analytics.logEvent(EVENT_SCREEN_VIEW, params)
    }
}

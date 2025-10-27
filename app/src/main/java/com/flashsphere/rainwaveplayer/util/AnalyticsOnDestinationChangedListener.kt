package com.flashsphere.rainwaveplayer.util

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.EVENT_SCREEN_VIEW
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.SCREEN_CLASS
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.SCREEN_NAME

class AnalyticsOnDestinationChangedListener(
    private val analytics: Analytics,
): NavController.OnDestinationChangedListener {
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val params = Bundle().also {
            val route = destination.route?.substringBefore("/")
            it.putString(SCREEN_NAME, route?.substringAfterLast("."))
            it.putString(SCREEN_CLASS, route)

            arguments?.let { bundle ->
                destination.arguments.forEach { (t, u) ->
                    it.putString(t, u.type[bundle, t].toString())
                }
            }
        }
        analytics.logEvent(EVENT_SCREEN_VIEW, params)
    }
}

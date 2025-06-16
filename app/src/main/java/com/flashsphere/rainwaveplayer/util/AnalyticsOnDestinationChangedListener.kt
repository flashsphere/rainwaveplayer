package com.flashsphere.rainwaveplayer.util

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.google.firebase.analytics.FirebaseAnalytics

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
            it.putString(FirebaseAnalytics.Param.SCREEN_NAME, route?.substringAfterLast("."))
            it.putString(FirebaseAnalytics.Param.SCREEN_CLASS, route)

            arguments?.let { bundle ->
                destination.arguments.forEach { (t, u) ->
                    it.putString(t, u.type[bundle, t].toString())
                }
            }
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }
}

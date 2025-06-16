package com.flashsphere.rainwaveplayer.cast

import android.content.Context
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.util.ClassUtils.getClassName
import com.flashsphere.rainwaveplayer.view.activity.MainActivity
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.tv.CastReceiverOptions
import com.google.android.gms.cast.tv.ReceiverOptionsProvider
import timber.log.Timber

class CastOptionsProvider : OptionsProvider, ReceiverOptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        Timber.d("getCastOptions")
        val activityClassName = MainActivity::class.getClassName()
        val buttonActions = listOf(
            MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
            MediaIntentReceiver.ACTION_STOP_CASTING
        )
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(activityClassName)
            .setActions(buttonActions, intArrayOf(0, 1))
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(activityClassName)
            .build()
        val launchOptions = LaunchOptions.Builder()
            .setAndroidReceiverCompatible(true)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.cast_app_id))
            .setCastMediaOptions(mediaOptions)
            .setLaunchOptions(launchOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }

    override fun getOptions(context: Context): CastReceiverOptions {
        Timber.d("getReceiverOptions")
        return CastReceiverOptions.Builder(context)
            .build()
    }
}

package com.flashsphere.rainwaveplayer.view.activity

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.app.RainwaveApp
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.EVENT_SCREEN_VIEW
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.SCREEN_CLASS
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.SCREEN_NAME
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.jakewharton.processphoenix.ProcessPhoenix
import timber.log.Timber
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {
    @Inject
    lateinit var dataStore: DataStore<Preferences>
    @Inject
    lateinit var analytics: Analytics

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        // check if app is launched in restricted mode (due to auto backup) and kill the process
        // https://issuetracker.google.com/issues/160946170#comment8
        if (application !is RainwaveApp) {
            ProcessPhoenix.triggerRebirth(this)
            return
        }

        Timber.tag(getSimpleClassName()).d("Creating")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.argb(0x80, 0x1b, 0x1b, 0x1b))
        )
        super.onCreate(savedInstanceState)
    }

    @CallSuper
    override fun onDestroy() {
        Timber.tag(getSimpleClassName()).d("Destroying")
        super.onDestroy()
    }

    fun setContent(
        screenName: String,
        screenClass: String = javaClass.name,
        bundle: Bundle? = null,
        content: @Composable () -> Unit
    ) {
        setContent {
            LaunchedEffect(Unit) {
                trackScreenView(screenName, screenClass, bundle)
            }
            content()
        }
    }

    private fun trackScreenView(screenName: String, screenClass: String = screenName, bundle: Bundle? = null) {
        val params = Bundle().also {
            it.putString(SCREEN_NAME, screenName)
            it.putString(SCREEN_CLASS, screenClass)

            if (bundle != null) {
                it.putAll(bundle)
            }
        }

        analytics.logEvent(EVENT_SCREEN_VIEW, params)
    }

    fun startMediaPlaybackFromSearch(query: String) {
        Timber.d("startMediaPlaybackFromSearch")
        MediaService.playFromSearch(this, query)
    }
}

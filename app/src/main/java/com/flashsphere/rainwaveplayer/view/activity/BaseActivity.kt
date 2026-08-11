package com.flashsphere.rainwaveplayer.view.activity

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.app.RainwaveApp
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.jakewharton.processphoenix.ProcessPhoenix
import jakarta.inject.Inject
import timber.log.Timber

abstract class BaseActivity : AppCompatActivity() {
    @Inject
    lateinit var dataStore: DataStore<Preferences>

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

    fun startMediaPlaybackFromSearch(query: String) {
        Timber.d("startMediaPlaybackFromSearch")
        MediaService.playFromSearch(this, query)
    }
}

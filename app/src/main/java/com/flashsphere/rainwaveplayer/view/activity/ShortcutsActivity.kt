package com.flashsphere.rainwaveplayer.view.activity

import android.content.Intent
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.lifecycleScope
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.util.Analytics
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : BaseActivity() {

    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onStart() {
        super.onStart()

        Timber.d("intent action = %s", intent.action)

        if (intent.action != "rainwave.intent.action.PLAY") {
            goToHome()
            return
        }

        ShortcutManagerCompat.reportShortcutUsed(applicationContext, "rainwave.shortcut.play")
        analytics.logEvent(Analytics.EVENT_USE_SHORTCUT)

        if (!mediaPlayerStateObserver.currentState.isStopped()) {
            goToHome()
            return
        }

        lifecycleScope.launchWithDefaults("Play from Shortcut") {
            playbackManager.play()
            goToHome()
        }
    }

    private fun goToHome() {
        val goToHomeIntent = Intent(Intent.ACTION_MAIN)
        goToHomeIntent.addCategory(Intent.CATEGORY_HOME)
        goToHomeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(goToHomeIntent)
        finish()
    }
}

package com.flashsphere.rainwaveplayer.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.view.viewmodel.StoreUserCredentialsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class StoreUserCredentialsActivity : BaseActivity() {

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var playbackManager: PlaybackManager

    private val viewModel: StoreUserCredentialsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.userCredentialsSaved
            .filterNotNull()
            .onEach { userCredentialsSaved ->
                if (userCredentialsSaved) {
                    onStoreCredentialsSuccess()
                } else {
                    onStoreCredentialsFailed()
                }
            }
            .launchWithDefaults(lifecycleScope, "Store User Credentials")
    }

    override fun onStart() {
        super.onStart()

        val intent = intent
        val uri = intent.data
        if (uri == null) {
            showNowPlayingActivity()
            return
        }

        val userCredentials = userRepository.parseCredentialsUri(uri)

        if (userCredentials == null) {
            onStoreCredentialsFailed()
            return
        }

        analytics.logEvent(Analytics.EVENT_LOGIN)

        viewModel.saveUserCredentials(userCredentials.userId, userCredentials.apiKey)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun onStoreCredentialsSuccess() {
        stationRepository.clearCache()
        showNowPlayingActivity()
        restartPlayback()
    }

    private fun onStoreCredentialsFailed() {
        Toast.makeText(this, R.string.error_save_credentials_failed, Toast.LENGTH_LONG).show()
        showNowPlayingActivity()
    }

    private fun showNowPlayingActivity() {
        MainActivity.startActivity(this)
        finish()
    }

    private fun restartPlayback() {
        if (!mediaPlayerStateObserver.currentState.isStopped()) {
            coroutineDispatchers.scope.launchWithDefaults("Restart Playback After Login") {
                suspendRunCatching { playbackManager.play() }
            }
        }
    }

    companion object {
        fun getCallingIntent(context: Context, url: String): Intent {
            val intent = Intent(context, StoreUserCredentialsActivity::class.java)
            intent.data = url.toUri()
            return intent
        }
    }
}

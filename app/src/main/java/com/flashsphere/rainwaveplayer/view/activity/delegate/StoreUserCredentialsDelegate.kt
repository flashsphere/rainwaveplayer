package com.flashsphere.rainwaveplayer.view.activity.delegate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.view.activity.MainActivity
import com.flashsphere.rainwaveplayer.view.viewmodel.StoreUserCredentialsViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

class StoreUserCredentialsDelegate(
    private val context: Context,
    private val viewModel: StoreUserCredentialsViewModel,
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
    private val playbackManager: PlaybackManager,
    private val analytics: Analytics,
    private val finishActivity: () -> Unit,
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        viewModel.userCredentialsSaved
            .filterNotNull()
            .onEach { userCredentialsSaved ->
                if (userCredentialsSaved) {
                    onStoreCredentialsSuccess()
                } else {
                    onStoreCredentialsFailed()
                }
            }
            .catch { onStoreCredentialsFailed() }
            .launchWithDefaults(owner.lifecycleScope, "Store User Credentials")
    }

    fun process(intent: Intent) {
        process(intent.data)
    }

    fun process(uri: Uri?) {
        if (uri == null) {
            showMainActivity()
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

    private suspend fun onStoreCredentialsSuccess() {
        stationRepository.clearCache()
        restartPlayback()
        showMainActivity()
    }

    private fun onStoreCredentialsFailed() {
        Toast.makeText(context, R.string.error_save_credentials_failed, Toast.LENGTH_LONG).show()
        showMainActivity()
    }

    private fun showMainActivity() {
        MainActivity.startActivity(context)
        finishActivity()
    }

    private suspend fun restartPlayback() {
        if (!mediaPlayerStateObserver.currentState.isStopped()) {
            playbackManager.play()
        }
    }
}

package com.flashsphere.rainwaveplayer.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.view.activity.delegate.StoreUserCredentialsDelegate
import com.flashsphere.rainwaveplayer.view.viewmodel.StoreUserCredentialsViewModel
import dagger.hilt.android.AndroidEntryPoint
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
    lateinit var playbackManager: PlaybackManager

    private lateinit var storeUserCredentialsDelegate: StoreUserCredentialsDelegate

    private val viewModel: StoreUserCredentialsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storeUserCredentialsDelegate = StoreUserCredentialsDelegate(this, viewModel,
            stationRepository, userRepository, mediaPlayerStateObserver, playbackManager,
            analytics, this::finish)
        lifecycle.addObserver(storeUserCredentialsDelegate)
    }

    override fun onStart() {
        super.onStart()
        storeUserCredentialsDelegate.process(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        fun getCallingIntent(context: Context, url: String): Intent {
            val intent = Intent(context, StoreUserCredentialsActivity::class.java)
            intent.data = url.toUri()
            return intent
        }
    }
}

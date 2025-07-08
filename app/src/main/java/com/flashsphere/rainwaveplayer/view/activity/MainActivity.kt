package com.flashsphere.rainwaveplayer.view.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.cast.CastContextHolder
import com.flashsphere.rainwaveplayer.cast.CastReceiverContextHolder
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.service.MediaServiceConnection
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.IntentUtils
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.util.isTv
import com.flashsphere.rainwaveplayer.view.activity.delegate.MainActivityDelegate
import com.flashsphere.rainwaveplayer.view.activity.delegate.NonTvMainActivityDelegate
import com.flashsphere.rainwaveplayer.view.activity.delegate.TvMainActivityDelegate
import com.flashsphere.rainwaveplayer.view.helper.CustomTabsUtil
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject
    lateinit var stationRepository: StationRepository
    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver
    @Inject
    lateinit var castContextHolder: CastContextHolder
    @Inject
    lateinit var castReceiverContextHolder: CastReceiverContextHolder
    @Inject
    lateinit var playbackManager: PlaybackManager
    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers
    @Inject
    lateinit var mediaServiceConnection: MediaServiceConnection
    @Inject
    lateinit var json: Json

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var delegate: MainActivityDelegate

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setupSplashScreen()
        super.onCreate(savedInstanceState)

        val drawerItemHandler = createDrawerItemHandler()
        delegate = if (!isTv()) {
            NonTvMainActivityDelegate(this, mainViewModel, drawerItemHandler)
        } else {
            TvMainActivityDelegate(this, mainViewModel, drawerItemHandler)
        }
        delegate.onCreate(savedInstanceState)

        subscribeToStationsScreenState()

        mainViewModel.getStations()
    }

    override fun onDestroy() {
        delegate.onDestroy()
        super.onDestroy()
    }

    private fun setupSplashScreen() {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { mainViewModel.shouldKeepSplashScreenOn() }
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("onNewIntent")
        super.onNewIntent(intent)
        setIntent(intent)

        Timber.d("onNewIntent intent action = %s", intent.action)

        if (delegate.onNewIntent(intent)) {
            return
        }

        IntentUtils.getParcelableExtra(intent, INTENT_EXTRA_PARAM_STATION, Station::class.java)?.let {
            Timber.d("Intent has Station %s", it.name)
            mainViewModel.station(it)
            return
        }

        handleVoiceSearch(intent)
    }

    private fun handleVoiceSearch(intent: Intent): Boolean {
        if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH != intent.action) {
            return false
        }
        Timber.d("handleVoiceSearch")

        val searchQuery = intent.extras?.getString(SearchManager.QUERY)
        if (searchQuery.isNullOrBlank()) {
            return false
        }
        startMediaPlaybackFromSearch(searchQuery)

        return true
    }

    private fun subscribeToStationsScreenState() {
        mainViewModel.stationsScreenState
            .onEach { (_, _, error) ->
                if (error != null && error.type == OperationError.Unauthorized) {
                    showUnauthorized(error.message!!)
                }
            }
            .map { it.stations }
            .filterNotNull()
            .filter { it.isNotEmpty() }
            .onEach { delegate.onStationsLoaded(it) }
            .launchWithDefaults(lifecycleScope, "Stations List State in Main activity")
    }

    private fun showUnauthorized(message: String) {
        Toast.makeText(this, getString(R.string.error_unauthorized, message), Toast.LENGTH_LONG).show()
        logout()
    }

    private fun logout() {
        lifecycleScope.launch {
            playbackManager.stop()

            userRepository.logout()
            stationRepository.clearCache()

            startActivity(this@MainActivity)
            finish()
        }
    }

    private fun createDrawerItemHandler(): DrawerItemHandler = DrawerItemHandler(
        stationClick = { mainViewModel.station(it) },
        allFavesClick = {
            AllFavesActivity.startActivity(this)
        },
        recentVotesClick = {
            mainViewModel.station.value?.let {
                RecentVotesActivity.startActivity(this, it)
            }
        },
        requestHistoryClick = {
            mainViewModel.station.value?.let {
                RequestHistoryActivity.startActivity(this, it)
            }
        },
        discordClick = {
            analytics.logEvent(Analytics.EVENT_OPEN_DISCORD)
            CustomTabsUtil.openDiscordPage(this)
        },
        patreonClick = {
            analytics.logEvent(Analytics.EVENT_OPEN_PATREON)
            CustomTabsUtil.openPatreonPage(this)
        },
        sleepTimerClick = { mainViewModel.showSleepTimer.value = true },
        settingsClick = { SettingsActivity.startActivity(this) },
        aboutClick = { AboutActivity.startActivity(this) },
        loginClick = { CustomTabsUtil.openLoginPage(this) },
        logoutClick = this::logout,
    )

    companion object {
        private const val INTENT_EXTRA_PARAM_STATION = "com.flashsphere.data.station"

        fun startActivity(context: Context, station: Station? = null) {
            context.startActivity(getCallingIntent(context, station))
        }

        fun getCallingIntent(context: Context, station: Station? = null): Intent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_NEW_TASK)
            station?.let {
                intent.putExtra(INTENT_EXTRA_PARAM_STATION, it)
            }
            return intent
        }
    }
}

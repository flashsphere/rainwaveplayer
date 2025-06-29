package com.flashsphere.rainwaveplayer.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.autovote.v1.RuleParams
import com.flashsphere.rainwaveplayer.cast.CastReceiverContextHolder
import com.flashsphere.rainwaveplayer.coroutine.coroutineExceptionHandler
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.media.MediaNotificationHelper
import com.flashsphere.rainwaveplayer.media.MediaNotificationHelper.Companion.FOREGROUND_SERVICE_TYPE
import com.flashsphere.rainwaveplayer.media.MediaNotificationHelper.Companion.NOTIFICATION_ID
import com.flashsphere.rainwaveplayer.media.MediaSessionHelper
import com.flashsphere.rainwaveplayer.media.VoteSongNotificationHelper
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoErrorResponse
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.network.NetworkManager
import com.flashsphere.rainwaveplayer.playback.LocalPlayback
import com.flashsphere.rainwaveplayer.playback.Playback
import com.flashsphere.rainwaveplayer.receiver.FavoriteSongIntentHandler
import com.flashsphere.rainwaveplayer.receiver.VoteSongIntentHandler
import com.flashsphere.rainwaveplayer.repository.RulesRepository
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.ErrorUtils
import com.flashsphere.rainwaveplayer.util.ErrorUtils.isRetryable
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.viewmodel.FaveSongDelegate
import com.flashsphere.rainwaveplayer.view.viewmodel.FaveSongState
import com.flashsphere.rainwaveplayer.view.viewmodel.VoteSongDelegate
import com.flashsphere.rainwaveplayer.view.viewmodel.VoteSongState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MediaService : MediaBrowserServiceCompat(), Playback.Callback, LifecycleOwner {

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    @Named("info_error_response_converter")
    lateinit var infoErrorResponseConverter: Converter<ResponseBody, InfoErrorResponse>

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var faveSongDelegate: FaveSongDelegate

    @Inject
    lateinit var voteSongDelegate: VoteSongDelegate

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var rulesRepository: RulesRepository

    @Inject
    lateinit var castReceiverContextHolder: CastReceiverContextHolder

    @Inject
    lateinit var networkManager: NetworkManager

    private lateinit var serviceScope: CoroutineScope
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var playback: Playback
    private lateinit var mediaSessionHelper: MediaSessionHelper
    private lateinit var mediaNotificationHelper: MediaNotificationHelper
    private lateinit var voteSongNotificationHelper: VoteSongNotificationHelper
    private lateinit var favoriteSongIntentHandler: FavoriteSongIntentHandler
    private lateinit var voteSongIntentHandler: VoteSongIntentHandler

    private val localBinder = LocalBinder(this)
    private val bound = AtomicInteger(0)

    private var stationsJob: Job? = null
    private var suggestedJob: Job? = null
    private var lastPlayedJob: Job? = null
    private var connectivityJob: Job? = null
    private var notificationJob: Job? = null
    private var playJob: Job? = null
    private var autoRequestJob: Job? = null
    private var autoVoteJob: Job? = null
    private var stateChangeEventsJob: Job? = null

    private var currentStation: Station? = null

    private val dispatcher = ServiceLifecycleDispatcher(this)
    private val handler = Handler(Looper.getMainLooper())
    private val stopPlaybackRunnable = Runnable { processStopRequest() }

    val mediaSession get() = mediaSessionHelper.getMediaSession()

    override val lifecycle
        get() = dispatcher.lifecycle

    override fun onCreate() {
        Timber.d("onCreate")
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()

        lifecycle.addObserver(networkManager)

        serviceScope = CoroutineScope(coroutineDispatchers.main + SupervisorJob() + coroutineExceptionHandler)
        notificationManager = NotificationManagerCompat.from(this)

        mediaSessionHelper = MediaSessionHelper(this, userRepository, mediaPlayerStateObserver,
            castReceiverContextHolder)
        mediaNotificationHelper = MediaNotificationHelper(this, mediaSessionHelper, userRepository)
        voteSongNotificationHelper = VoteSongNotificationHelper(this, serviceScope,
            userRepository, dataStore)
        favoriteSongIntentHandler = FavoriteSongIntentHandler(serviceScope, faveSongDelegate)
        voteSongIntentHandler = VoteSongIntentHandler(serviceScope, voteSongDelegate, analytics)

        playback = LocalPlayback(this, serviceScope, stationRepository, okHttpClient,
            dataStore, networkManager)
        playback.setCallback(this)

        sessionToken = mediaSessionHelper.getMediaSession().sessionToken
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        cleanupService()

        mediaSessionHelper.destroy()

        serviceScope.cancel()

        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        Timber.d("onGetRoot clientPackageName=%s; clientUid=%d; rootHints=%s", clientPackageName, clientUid, rootHints)
        val isSuggestedRequest = rootHints?.getBoolean(BrowserRoot.EXTRA_SUGGESTED) == true
        val isOfflineRequest = rootHints?.getBoolean(BrowserRoot.EXTRA_OFFLINE) == true
        val isRecentRequest = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) == true

        val browserRootPath = if (ALLOWED_RECENT_PACKAGES.contains(clientPackageName) && isRecentRequest) {
            return null
        } else if (!ALLOWED_GET_ROOT_PACKAGES.contains(clientPackageName)) {
            EMPTY_ROOT
        } else if (isOfflineRequest) {
            return null
        } else if (isSuggestedRequest) {
            SUGGESTED_ROOT
        } else {
            BROWSABLE_ROOT
        }

        val rootExtras = Bundle().apply {
            putBoolean(BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, false)
            putInt(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        }

        return BrowserRoot(browserRootPath, rootExtras)
    }

    override fun onLoadChildren(parentId: String, mediaItems: Result<List<MediaBrowserCompat.MediaItem>>) {
        Timber.d("onLoadChildren parentId=%s", parentId)

        when (parentId) {
            BROWSABLE_ROOT -> {
                val collector = MediaBrowserItemsCollector(parentId, stationRepository, mediaItems)

                cancel(stationsJob)
                stationsJob = flow { emit(stationRepository.getStations()) }
                    .autoRetry(connectivityObserver, coroutineDispatchers)
                    .catch { collector.process(it) }
                    .onEach { collector.process(it) }
                    .launchWithDefaults(serviceScope, "Stations for Browsable Root")
            }
            SUGGESTED_ROOT -> {
                val collector = MediaBrowserItemsCollector(parentId, stationRepository, mediaItems)

                cancel(suggestedJob)
                suggestedJob = flow { emit(stationRepository.getSuggestedStations()) }
                    .autoRetry(connectivityObserver, coroutineDispatchers)
                    .catch { collector.process(it) }
                    .onEach { collector.process(it) }
                    .launchWithDefaults(serviceScope, "Stations for Suggested Root")
            }
            RECENT_ROOT -> {
                val collector = MediaBrowserItemsCollector(parentId, stationRepository, mediaItems)

                cancel(lastPlayedJob)
                lastPlayedJob = flow { emit(stationRepository.getLastPlayedStationWithoutDefault()) }
                    .autoRetry(connectivityObserver, coroutineDispatchers)
                    .map { if (it != null) listOf(it) else emptyList() }
                    .catch { collector.process(it) }
                    .onEach { collector.process(it) }
                    .launchWithDefaults(serviceScope, "Stations for Recent Root")
            }
            EMPTY_ROOT -> {
                Timber.d("onLoadChildren for empty root")
                mediaItems.sendResult(emptyList())
            }
            else -> {
                Timber.d("onLoadChildren for unknown root")
                mediaItems.sendResult(null)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        Timber.d("onBind action = %s", intent?.action)
        if (SERVICE_INTERFACE == intent?.action) {
            return super.onBind(intent)
        }
        bound.incrementAndGet()
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        Timber.d("onRebind action = %s", intent?.action)
        if (SERVICE_INTERFACE == intent?.action) {
            return super.onRebind(intent)
        }
        bound.incrementAndGet()
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("onUnbind action = %s", intent?.action)
        if (SERVICE_INTERFACE == intent?.action) {
            return super.onUnbind(intent)
        }
        bound.decrementAndGet()
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dispatcher.onServicePreSuperOnStart()

        if (intent == null || intent.action.isNullOrBlank()) {
            return START_NOT_STICKY
        }

        Timber.i("media service intent action = %s", intent.action)

        if (handleMediaButtonIntent(intent)) {
            return START_NOT_STICKY
        }

        if (favoriteSongIntentHandler.handleIntent(intent)) {
            return START_NOT_STICKY
        }

        if (voteSongIntentHandler.handleIntent(intent)) {
            return START_NOT_STICKY
        }

        handleIntent(intent)
        return START_NOT_STICKY
    }

    private fun handleMediaButtonIntent(intent: Intent): Boolean {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action && !intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            return false
        }

        val ke = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return true

        val mediaController = mediaSessionHelper.getMediaSession().controller
        mediaController.dispatchMediaButtonEvent(ke)
        return true
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> {
                playLastPlayedStation()
            }
            ACTION_PLAY_FROM_MEDIA_ID -> {
                findAndPlayStationUsingMediaId(intent.getStringExtra(EXTRA_PARAM_QUERY))
            }
            ACTION_PLAY_FROM_SEARCH_QUERY -> {
                findAndPlayStationUsingQuery(intent.getStringExtra(EXTRA_PARAM_QUERY))
            }
            ACTION_PLAY_FROM_STATION -> {
                IntentCompat.getParcelableExtra(intent, EXTRA_PARAM_STATION, Station::class.java)?.let {
                    processPlayRequest(it)
                }
            }
            ACTION_PREV -> {
                processSkipToPrevRequest()
            }
            ACTION_NEXT -> {
                processSkipToNextRequest()
            }
            ACTION_STOP -> {
                processStopRequest()
            }
            ACTION_PAUSE -> {
                processPauseRequest()
            }
        }
    }

    private fun disposeSubscriptions() {
        Timber.d("disposeSubscriptions")
        cancel(playJob, connectivityJob, notificationJob, autoRequestJob, autoVoteJob, stateChangeEventsJob)
        voteSongNotificationHelper.removeNotifications()
        removeStopPlaybackRunnable()
    }

    private fun relaxResources() {
        Timber.d("relaxResources")
        disposeSubscriptions()
        playback.stop()
    }

    private fun addStopPlaybackRunnable() {
        removeStopPlaybackRunnable()
        handler.postDelayed(stopPlaybackRunnable, CLEANUP_DELAY_MS)
    }

    private fun removeStopPlaybackRunnable() {
        handler.removeCallbacks(stopPlaybackRunnable)
    }

    private fun startForegroundService(notification: Notification) {
        runCatching {
            postNotification(notification)

            Timber.i("Moving service to foreground")
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE)
        }.onFailure {
            Timber.e(it, "Can't move service to foreground")
        }
    }

    private fun postNotification(notification: Notification) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun launchPlayJob(name: String, block: suspend () -> Station) {
        cancel(playJob)
        playJob = serviceScope.launchWithDefaults(name) {
            suspendRunCatching(block)
                .onFailure { processStopRequest() }
                .onSuccess { result ->
                    currentStation = result
                    processPlayRequest()
                }
        }
    }

    private fun playLastPlayedStation() {
        Timber.i("playLastPlayedStation")
        mediaSessionHelper.setConnectingState()
        startForegroundService(mediaNotificationHelper.createConnectingNotification())
        launchPlayJob("Find Last Played Station") { stationRepository.getLastPlayedStationWithDefault() }
    }

    private fun processSkipToPrevRequest() {
        Timber.i("processSkipToPrevRequest")
        mediaSessionHelper.setConnectingState()
        startForegroundService(mediaNotificationHelper.createConnectingNotification())
        launchPlayJob("Find Prev Station") { stationRepository.getPrevStation(currentStation) }
    }

    private fun processSkipToNextRequest() {
        Timber.i("processSkipToNextRequest")
        mediaSessionHelper.setConnectingState()
        startForegroundService(mediaNotificationHelper.createConnectingNotification())
        launchPlayJob("Find Next Station") { stationRepository.getNextStation(currentStation) }
    }

    private fun findAndPlayStationUsingQuery(query: String?) {
        Timber.i("processPlayRequestWithQuery query = %s", query)
        mediaSessionHelper.setConnectingState()
        startForegroundService(mediaNotificationHelper.createConnectingNotification())
        launchPlayJob("Find Station With Query") { stationRepository.getStationWithQuery(query) }
    }

    private fun findAndPlayStationUsingMediaId(mediaId: String?) {
        Timber.i("processPlayRequestWithQuery mediaId = %s", mediaId)
        mediaSessionHelper.setConnectingState()
        startForegroundService(mediaNotificationHelper.createConnectingNotification())
        launchPlayJob("Find Station With Media Id") { stationRepository.getStationWithId(mediaId) }
    }

    private fun processPlayRequest(station: Station) {
        Timber.i("processPlayRequest station id = %d", station.id)
        mediaSessionHelper.setConnectingState()
        startForegroundService(mediaNotificationHelper.createConnectingNotification())

        currentStation = station
        processPlayRequest()
    }

    private fun processPlayRequest() {
        Timber.d("processPlayRequest")
        analytics.logEvent(Analytics.EVENT_PLAY_REQUEST, currentStation)
        play()
    }

    private fun processPauseRequest() {
        Timber.d("processPauseRequest")

        if (currentStation == null) {
            cleanupService()
        } else {
            relaxResources()
            showPaused()
        }
    }

    private fun showPaused() {
        Timber.i("showPaused")
        currentStation?.let {
            disposeSubscriptions()

            if (!mediaPlayerStateObserver.currentState.isStopped()) {
                stationRepository.refreshStationInfo(it.id, 10)
            }

            startForegroundService(mediaNotificationHelper.showStoppedNotification(it))
            mediaSessionHelper.setPausedState(it)
        }
        addStopPlaybackRunnable()
    }

    private fun processStopRequest() {
        Timber.d("processStopRequest")
        cleanupService()
        stopSelf()
    }

    private fun cleanupService() {
        Timber.d("cleanupService")
        relaxResources()
        currentStation.let {
            if (it == null) {
                mediaSessionHelper.setStoppedState()
            } else {
                if (!mediaPlayerStateObserver.currentState.isStopped()) {
                    stationRepository.refreshStationInfo(it.id, 10)
                }
                mediaSessionHelper.setStoppedState(it)
            }
        }
        Timber.i("Removing service from foreground")
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun play() {
        Timber.d("play")
        relaxResources()

        currentStation?.let {
            startForegroundService(mediaNotificationHelper.createConnectingNotification(it))
            mediaSessionHelper.setConnectingState(it)

            stationRepository.saveLastPlayedStation(it)
            playback.play(it)
        }
    }

    private fun setupPlayingSubscriptions(station: Station) {
        Timber.d("setupNotification")
        setupNotificationSubscription(station)
        setupEventBusSubscription(station)
    }

    private fun setupNotificationSubscription(station: Station) {
        Timber.d("setupNotificationSubscription")
        cancel(notificationJob)
        notificationJob = stationRepository.getStationInfoFlow(station.id, true)
            .distinctUntilChangedBy { it.currentEvent.id }
            .catch { e ->
                Timber.w("Error getting song info for notification")
                currentStation?.let {
                    startForegroundService(mediaNotificationHelper.createPlayingNotification(it))
                    mediaSessionHelper.setPlayingState(it)
                }
                throw e
            }
            .retry { throwable ->
                // don't retry on unauthorized errors
                return@retry if (throwable is HttpException && throwable.code() == 403) {
                    Timber.d("http %d exception", throwable.code())
                    false
                } else {
                    Timber.e(throwable, "Retry getting song info for notification")
                    delay(1.seconds)
                    true
                }
            }
            .catch { throwable ->
                val error = ErrorUtils.extractError(throwable, infoErrorResponseConverter)
                if (error.type == OperationError.Unauthorized) {
                    userRepository.logout()
                    stationRepository.clearCache()
                    createAndShowToast(getString(R.string.error_unauthorized, throwable.message))
                } else {
                    createAndShowToast(getString(R.string.error_connection))
                }
                processStopRequest()
            }
            .onEach { infoResponse ->
                Timber.i("Showing song info for notification")
                showNotification(station, infoResponse)

                startAutoRequest(station.id, infoResponse)
                startAutoVote(station, infoResponse)
            }
            .launchWithDefaults(serviceScope, "Station Info for Media Notification")
    }

    private fun startAutoRequest(stationId: Int, infoResponse: InfoResponse) {
        if (userRepository.isLoggedIn()) {
            cancel(autoRequestJob)
            autoRequestJob = serviceScope.launchWithDefaults("Auto Request") {
                suspendRunCatching { stationRepository.autoRequestSongs(stationId, infoResponse) }
            }
        }
    }

    private fun startAutoVote(station: Station, infoResponse: InfoResponse) {
        val userCredentials = userRepository.getCredentials() ?: return
        val stationId = station.id

        cancel(autoVoteJob)

        val event = infoResponse.futureEvents.getOrNull(0) ?: return
        autoVoteJob = serviceScope.launchWithDefaults("Auto Vote") {
            suspendRunCatching {
                withContext(coroutineDispatchers.compute) {
                    val rules = rulesRepository.get()
                    if (rules.isEmpty()) {
                        return@withContext
                    }

                    val ruleParams = RuleParams(userCredentials.userId)
                    val song = rules.asSequence().map { it.apply(event, ruleParams) }
                        .filterNotNull()
                        .firstOrNull() ?: return@withContext

                    if (!song.voted) {
                        Timber.d("voting for song id: %d", song.id)
                        voteSongDelegate.voteSong(stationId, event.id, song.entryId)
                    }
                }
            }.mapCatching {
                voteSongNotificationHelper.showNotifications(station, event)
            }
        }
    }

    private suspend fun fetchImage(song: Song): Bitmap? {
        var bitmap: Bitmap? = null
        imageLoader.execute(ImageRequest.Builder(this)
            .data(song.getAlbumCoverUrl())
            .target(
                onStart = {},
                onSuccess = { result -> bitmap = result.toBitmap() },
                onError = {}
            )
            .build())

        return bitmap
    }

    private suspend fun showNotification(station: Station, result: InfoResponse) {
        val song = result.getCurrentSong()

        val bitmap = fetchImage(song)
        if (bitmap != null) {
            startForegroundService(mediaNotificationHelper.createSongNotification(station, song, bitmap))
            mediaSessionHelper.setPlayingState(station, result, bitmap)
        } else {
            startForegroundService(mediaNotificationHelper.createSongNotification(station, song))
            mediaSessionHelper.setPlayingState(station, result)
        }
    }

    private fun createAndShowToast(message: String) {
        if (message.isNotBlank()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupEventBusSubscription(station: Station) {
        cancel(stateChangeEventsJob)
        stateChangeEventsJob = SupervisorJob().also { job ->
            faveSongDelegate.faveSongState
                .filter { it.success }
                .onEach { handleFavoriteCurrentSongChange(station, it) }
                .launchWithDefaults(serviceScope + job, "Fave Song State Changed in Media Service")

            voteSongDelegate.voteSongState
                .filter { it.stationId == station.id }
                .onEach { handleVoteSongEvent(it) }
                .launchWithDefaults(serviceScope + job, "Vote Song State Changed in Media Service")
        }
    }

    private fun handleVoteSongEvent(state: VoteSongState) {
        if (state.success) {
            voteSongNotificationHelper.removeNotifications(state.eventId)
            return
        }

        // don't show messages via toast if service is bound to activity
        // since the message will be shown via the snackbar
        if (bound.get() > 0) {
            return
        }

        val infoResponse = stationRepository.getCurrentStationInfo(state.stationId) ?: return
        if (infoResponse.futureEvents[0].id != state.eventId) {
            return
        }

        val message = state.error?.message ?: getString(R.string.error_voting)
        createAndShowToast(message)
    }

    private suspend fun handleFavoriteCurrentSongChange(station: Station, state: FaveSongState) {
        val infoResponse = stationRepository.getCurrentStationInfo(station.id) ?: return
        val currentSong = infoResponse.getCurrentSong()
        if (currentSong.id == state.songId) {
            currentSong.favorite = state.favorite
            showNotification(station, infoResponse)
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        Timber.d("onPlaybackStatusChanged = %d", state)
        when (state) {
            PlaybackStateCompat.STATE_BUFFERING -> {
                Timber.i("Playback status changed. Buffering.")
                disposeSubscriptions()

                if (connectivityObserver.isConnected()) {
                    currentStation?.let {
                        startForegroundService(mediaNotificationHelper.createBufferingNotification(it))
                        mediaSessionHelper.setBufferingState(it)
                    }
                } else {
                    Timber.i("No connection")
                    handleNoConnection(ConnectException("No connection"))
                }
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                Timber.i("Playback status changed. Playing.")
                currentStation?.let {
                    startForegroundService(mediaNotificationHelper.createPlayingNotification(it))
                    mediaSessionHelper.setPlayingState(it)

                    setupPlayingSubscriptions(it)
                }
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                Timber.i("Playback status changed. Show paused.")
                showPaused()
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                Timber.i("Playback status changed. Stopping.")
                processStopRequest()
            }
        }
    }

    override fun onError(e: Exception) {
        Timber.w(e, "Playback error.")
        handleNoConnection(e)
    }

    private fun handleNoConnection(e: Exception) {
        relaxResources()
        addStopPlaybackRunnable()

        currentStation?.let {
            startForegroundService(mediaNotificationHelper.createWaitingForNetworkNotification(it))
            mediaSessionHelper.setWaitingForNetworkState(it)
        }

        cancel(connectivityJob)
        connectivityJob = flow { emit(isRetryable(e, connectivityObserver)) }
            .flowOn(coroutineDispatchers.compute)
            .onEach { canRetry ->
                if (canRetry) {
                    connectivityObserver.connectivityFlow.filter { it }.take(1)
                        .collect {
                            Timber.d("connected %s", it)
                            play()
                        }
                } else {
                    Timber.i(e, "Stopping playback")
                    if (!e.message.isNullOrBlank()) {
                        createAndShowToast(getString(R.string.error_connection_with_message, e.message))
                    } else {
                        createAndShowToast(getString(R.string.error_connection))
                    }
                    processStopRequest()
                }
            }
            .launchWithDefaults(serviceScope, "Connectivity Check to Resume Playback")
    }

    class LocalBinder(val service: MediaService) : Binder()

    companion object {
        private const val BROWSABLE_ROOT = "__ROOT__"
        private const val SUGGESTED_ROOT = "__SUGGESTED_ROOT__"
        private const val RECENT_ROOT = "__RECENT_ROOT__"
        private const val EMPTY_ROOT = "@empty@"

        private val ALLOWED_GET_ROOT_PACKAGES = listOf(
            "com.google.android.projection.gearhead",
            "com.google.android.wearable.app",
            "com.google.android.autosimulator",
            "com.google.android.googlequicksearchbox",
            "com.google.android.carassistant")
        private val ALLOWED_RECENT_PACKAGES = listOf("com.android.systemui")

        private const val ACTION_PLAY = "com.flashsphere.action.play"
        private const val ACTION_PLAY_FROM_STATION = "com.flashsphere.action.play_from_station"
        private const val ACTION_PLAY_FROM_MEDIA_ID = "com.flashsphere.action.play_from_media_id"
        private const val ACTION_PLAY_FROM_SEARCH_QUERY = "com.flashsphere.action.play_from_search_query"
        private const val ACTION_PREV = "com.flashsphere.action.previous"
        private const val ACTION_NEXT = "com.flashsphere.action.next"
        private const val ACTION_STOP = "com.flashsphere.action.stop"
        private const val ACTION_PAUSE = "com.flashsphere.action.pause"

        private const val EXTRA_PARAM_STATION = "com.flashsphere.data.station"
        private const val EXTRA_PARAM_QUERY = "com.flashsphere.data.query"

        private const val CLEANUP_DELAY_MS = 900000L // 15 minutes

        fun getPlayIntent(context: Context): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_PLAY)
        }

        fun play(context: Context) {
            startForegroundService(context, getPlayIntent(context))
        }

        fun playOrThrow(context: Context) {
            startForegroundServiceOrThrow(context, getPlayIntent(context))
        }

        fun getPlayFromMediaIdIntent(context: Context, mediaId: String?): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_PLAY_FROM_MEDIA_ID)
                .putExtra(EXTRA_PARAM_QUERY, mediaId)
        }

        fun playFromMediaId(context: Context, mediaId: String?) {
            startForegroundService(context, getPlayFromMediaIdIntent(context, mediaId))
        }

        fun getPlayFromSearchIntent(context: Context, query: String?): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_PLAY_FROM_SEARCH_QUERY)
                .putExtra(EXTRA_PARAM_QUERY, query)
        }

        fun playFromSearch(context: Context, query: String?) {
            startForegroundService(context, getPlayFromSearchIntent(context, query))
        }

        fun getPlayFromStationIntent(context: Context, station: Station): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_PLAY_FROM_STATION)
                .putExtra(EXTRA_PARAM_STATION, station)
        }

        fun playFromStation(context: Context, station: Station) {
            startForegroundService(context, getPlayFromStationIntent(context, station))
        }

        fun getSkipToPrevIntent(context: Context): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_PREV)
        }

        fun skipToPrev(context: Context) {
            startForegroundService(context, getSkipToPrevIntent(context))
        }

        fun getSkipToNextIntent(context: Context): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_NEXT)
        }

        fun skipToNext(context: Context) {
            startForegroundService(context, getSkipToNextIntent(context))
        }

        fun getPauseIntent(context: Context): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_PAUSE)
        }

        fun pause(context: Context) {
            runCatching { context.startService(getPauseIntent(context)) }
        }

        fun getStopIntent(context: Context): Intent {
            return Intent(context, MediaService::class.java)
                .setAction(ACTION_STOP)
        }

        fun stop(context: Context) {
            val intent = getStopIntent(context)
            runCatching {
                context.startService(intent)
            }.onFailure {
                context.stopService(Intent(context, MediaService::class.java))
            }
        }

        private fun startForegroundService(context: Context, intent: Intent) {
            runCatching {
                startForegroundServiceOrThrow(context, intent)
            }.onFailure {
                Timber.e(it, "Can't start foreground service")
            }
        }

        private fun startForegroundServiceOrThrow(context: Context, intent: Intent) {
            Timber.i("Starting foreground service")
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

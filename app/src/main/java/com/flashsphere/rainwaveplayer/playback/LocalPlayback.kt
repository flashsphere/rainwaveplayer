package com.flashsphere.rainwaveplayer.playback

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
import androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoTimeoutException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.broadcastReceiverFlow
import com.flashsphere.rainwaveplayer.media.Mp3ExtractorFactory
import com.flashsphere.rainwaveplayer.media.OggExtractorFactory
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.network.NetworkChangeCallback
import com.flashsphere.rainwaveplayer.network.NetworkManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.getBufferInMillis
import com.flashsphere.rainwaveplayer.util.listenUsingOgg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.math.max

@OptIn(UnstableApi::class) class LocalPlayback(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val stationRepository: StationRepository,
    private val okHttpClient: OkHttpClient,
    private val dataStore: DataStore<Preferences>,
    private val networkManager: NetworkManager,
) : Playback, Player.Listener, OnAudioFocusChangeListener, NetworkChangeCallback {

    private val context = context.applicationContext
    private val userAgent = Util.getUserAgent(this.context, context.getString(R.string.app_name))

    private val wifiLock: WifiManager.WifiLock
    private val wakeLock: PowerManager.WakeLock

    private var streamUrl: Uri = Uri.EMPTY
    private var exoPlayer: ExoPlayer? = null
    private var audioFocus = AudioFocus.NoFocusNoDuck
    private var playbackState = PlaybackStateCompat.STATE_STOPPED
    private var callback: Playback.Callback? = null

    private val audioFocusManager = AudioFocusManager(context, this)

    private var headsetJob: Job? = null

    init {
        val wifiManager = this.context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = createWifiLock(wifiManager)
        wifiLock.setReferenceCounted(false)

        val powerManager = this.context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_WAKE_LOCK)
        wakeLock.setReferenceCounted(false)
    }

    private fun createWifiLock(wifiManager: WifiManager): WifiManager.WifiLock {
        val lockType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        return wifiManager.createWifiLock(lockType, TAG_WIFI_LOCK)
    }

    override fun play(station: Station) {
        this.streamUrl = stationRepository.getRelayUrl(station).toUri()
        play(this.streamUrl)
    }

    private fun play(uri: Uri) {
        Timber.d("stream url = %s", uri)
        if (!wifiLock.isHeld) {
            Timber.d("acquire wifi lock")
            wifiLock.acquire()
        }
        if (!wakeLock.isHeld) {
            Timber.d("acquire wake lock")
            wakeLock.acquire()
        }
        observeHeadsets()
        tryToGetAudioFocus()
        if (audioFocus != AudioFocus.Focused) {
            stop()
            callback?.onPlaybackStateChanged(playbackState)
            return
        }
        networkManager.registerNetworkChangeCallback(this)
        setupExoPlayer(uri)
    }

    private fun cleanupExoPlayer() {
        exoPlayer?.let {
            Timber.d("cleanupExoPlayer")
            it.removeListener(this)
            it.stop()
            it.release()
        }
        exoPlayer = null
    }

    private fun setupExoPlayer(uri: Uri) {
        Timber.d("setupExoPlayer")
        cleanupExoPlayer()

        val factory = OkHttpDataSource.Factory(okHttpClient)
        factory.setUserAgent(userAgent)

        val mediaExtractorsFactory = if (dataStore.listenUsingOgg()) {
            OggExtractorFactory()
        } else {
            Mp3ExtractorFactory()
        }

        val mediaSource = ProgressiveMediaSource.Factory(factory, mediaExtractorsFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        val exoPlayer = createExoPlayer(mediaExtractorsFactory)
        this.exoPlayer = exoPlayer

        exoPlayer.addListener(this)
        exoPlayer.playWhenReady = true
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        Timber.i("Start connecting to station")
    }

    private fun createExoPlayer(mediaExtractorsFactory: ExtractorsFactory): ExoPlayer {
        val bufferMs = dataStore.getBufferInMillis()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                max(DEFAULT_MIN_BUFFER_MS, bufferMs),
                max(DEFAULT_MAX_BUFFER_MS, bufferMs),
                bufferMs,
                bufferMs)
            .build()

        return ExoPlayer.Builder(context, DefaultMediaSourceFactory(context, mediaExtractorsFactory))
            .setUsePlatformDiagnostics(false)
            .setLoadControl(loadControl)
            .build()
            .apply {
                val audioOffloadPrefs = AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                    .build()
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setAudioOffloadPreferences(audioOffloadPrefs)
                    .build()
            }
    }

    private fun observeHeadsets() {
        Timber.d("observeHeadsets")
        cancel(headsetJob)

        val intent = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        headsetJob = broadcastReceiverFlow(context, intent)
            .filter { AudioManager.ACTION_AUDIO_BECOMING_NOISY == it.action }
            .onEach {
                Timber.d("Headset is unplugged or disconnected.")
                // Stop the playback
                stop()
                callback?.onPlaybackStateChanged(playbackState)
            }
            .launchWithDefaults(coroutineScope, "Noisy Audio Broadcast Receiver")
    }

    override fun pause() {
        relaxResources()
        playbackState = PlaybackStateCompat.STATE_PAUSED
    }

    override fun stop() {
        relaxResources()
        giveUpAudioFocus()
        cancel(headsetJob)
        playbackState = PlaybackStateCompat.STATE_STOPPED
    }

    private fun relaxResources() {
        Timber.d("relaxResources")
        networkManager.unregisterNetworkChangeCallback(this)
        cleanupExoPlayer()

        if (wifiLock.isHeld) {
            Timber.d("release wifi lock")
            wifiLock.release()
        }
        if (wakeLock.isHeld) {
            Timber.d("release wake lock")
            wakeLock.release()
        }
    }

    override fun networkChanged() {
        setupExoPlayer(this.streamUrl)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.i("Gained audio focus")
                audioFocus = AudioFocus.Focused

                if (isPlaying()) {
                    configureAudioVolume()
                } else {
                    play(this.streamUrl)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Timber.i("Lost audio focus")
                audioFocus = AudioFocus.NoFocusNoDuck

                if (isPlaying()) {
                    pause()
                    callback?.onPlaybackStateChanged(playbackState)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.i("Lost audio focus, can duck")
                audioFocus = AudioFocus.NoFocusCanDuck
                configureAudioVolume()
            }
            else -> {
            }
        }
    }

    private fun tryToGetAudioFocus() {
        Timber.d("tryToGetAudioFocus")
        if (audioFocusManager.requestAudioFocus()) {
            Timber.d("tryToGetAudioFocus successful")
            audioFocus = AudioFocus.Focused
            return
        }
        Timber.d("tryToGetAudioFocus unsuccessful")
    }

    private fun giveUpAudioFocus() {
        Timber.d("giveUpAudioFocus")
        if (audioFocusManager.abandonAudioFocus()) {
            Timber.d("giveUpAudioFocus successful = true")
            audioFocus = AudioFocus.NoFocusNoDuck
        }
    }

    private fun configureAudioVolume() {
        Timber.d("configureAudioVolume() for audio focus: %s", audioFocus)
        when (audioFocus) {
            AudioFocus.NoFocusCanDuck -> exoPlayer?.volume = DUCK_VOLUME
            else -> {
                exoPlayer?.volume = 1F
            }
        }
    }

    private fun isPlaying(): Boolean {
        return this.playbackState == PlaybackStateCompat.STATE_BUFFERING
            || this.playbackState == PlaybackStateCompat.STATE_PLAYING
    }

    override fun setCallback(callback: Playback.Callback) {
        this.callback = callback
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        var text = "onPlayerStateChanged, playbackState="

        when (playbackState) {
            Player.STATE_BUFFERING -> {
                this.playbackState = PlaybackStateCompat.STATE_BUFFERING
                text += "buffering"
                callback?.onPlaybackStateChanged(this.playbackState)
            }
            Player.STATE_ENDED -> {
                text += "ended"
                play(this.streamUrl)
            }
            Player.STATE_IDLE -> text += "idle"
            Player.STATE_READY -> {
                this.playbackState = PlaybackStateCompat.STATE_PLAYING
                text += "ready"
                configureAudioVolume()
                callback?.onPlaybackStateChanged(this.playbackState)
            }
            else -> text += "unknown"
        }
        Timber.d(text)
    }

    override fun onPlayerError(e: PlaybackException) {
        val cause = e.cause
        if (cause != null &&
            cause is ExoTimeoutException &&
            cause.timeoutOperation == ExoTimeoutException.TIMEOUT_OPERATION_RELEASE) {
            return
        }
        Timber.e(e, "onPlayerError: %s", e.getSimpleClassName())

        relaxResources()
        callback?.onError(e)
    }

    companion object {
        private const val TAG_WIFI_LOCK = "rainwave:com.flashsphere.locks.wifi"
        private const val TAG_WAKE_LOCK = "rainwave:com.flashsphere.locks.wake"
        private const val DUCK_VOLUME = 0.1f
    }
}

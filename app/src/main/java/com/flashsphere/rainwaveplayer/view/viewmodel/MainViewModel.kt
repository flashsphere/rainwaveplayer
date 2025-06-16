package com.flashsphere.rainwaveplayer.view.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.station.StationsErrorResponse
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoErrorResponse
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.isTv
import com.flashsphere.rainwaveplayer.view.helper.SleepTimerDelegate
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import com.flashsphere.rainwaveplayer.view.uistate.StationInfoScreenState
import com.flashsphere.rainwaveplayer.view.uistate.StationsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveAlbumErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RateSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RateSongSuccessEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RefreshStationInfo
import com.flashsphere.rainwaveplayer.view.uistate.event.UserNotLoggedInEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.VoteSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfo
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import okhttp3.ResponseBody
import retrofit2.Converter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val dataStore: DataStore<Preferences>,
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
    private val uiEventDelegate: UiEventDelegate,
    private val faveSongDelegate: FaveSongDelegate,
    private val faveAlbumDelegate: FaveAlbumDelegate,
    private val voteSongDelegate: VoteSongDelegate,
    private val rateSongDelegate: RateSongDelegate,
    @Named("stations_error_response_converter")
    private val stationsErrorResponseConverter: Converter<ResponseBody, StationsErrorResponse>,
    @Named("info_error_response_converter")
    private val infoErrorResponseConverter: Converter<ResponseBody, InfoErrorResponse>,
    private val playbackManager: PlaybackManager,
    private val analytics: Analytics,
) : ViewModel() {
    private val isTv = context.isTv()
    private val sleepTimerDelegate = SleepTimerDelegate(context, dataStore, analytics)

    val snackbarEvents = uiEventDelegate.snackbarEvents
    val castState = MutableStateFlow("")

    private val _station = MutableStateFlow<Station?>(savedStateHandle[STATION_KEY])
    val station = _station.asStateFlow()

    private val _user = MutableStateFlow<UserState?>(savedStateHandle[USER_KEY])
    val user = _user.asStateFlow()

    private val _showPreviouslyPlayed = MutableStateFlow(false)
    val showPreviouslyPlayed = _showPreviouslyPlayed.asStateFlow()

    private val _stationsScreenState = MutableStateFlow(StationsScreenState())
    val stationsScreenState = _stationsScreenState.asStateFlow()

    private val _stationInfoScreenState = MutableStateFlow(StationInfoScreenState())
    val stationInfoScreenState = _stationInfoScreenState.asStateFlow()

    private val _playbackState = MutableStateFlow(mediaPlayerStateObserver.currentState)
    val playbackState = _playbackState.asStateFlow()

    val showSleepTimer = sleepTimerDelegate.showState

    private var stationsJob: Job? = null
    private var stationInfoJob: Job? = null
    private var refreshStationInfoJob: Job? = null
    private var faveSongStateJob: Job? = null
    private var faveAlbumStateJob: Job? = null
    private var voteSongStateJob: Job? = null
    private var rateSongStateJob: Job? = null
    private var playbackStateJob: Job? = null

    fun shouldKeepSplashScreenOn(): Boolean {
        val stationsScreenState = stationsScreenState.value
        return stationRepository.hasLocalCachedStations() &&
            stationsScreenState.stations == null && stationsScreenState.error == null
    }

    fun getStations() {
        _stationsScreenState.value = StationsScreenState.loading()

        cancel(stationsJob)
        stationsJob = flow { emit(stationRepository.getStations()) }
            .autoRetry(connectivityObserver, coroutineDispatchers) { e ->
                _stationsScreenState.value = StationsScreenState.error(e.toOperationError(stationsErrorResponseConverter))
            }
            .onEach { data ->
                _stationsScreenState.value = StationsScreenState.loaded(data)
            }
            .launchWithDefaults(viewModelScope, "Stations in Main VM")
    }

    fun station(station: Station) {
        if (station.id == _station.value?.id) return
        savedStateHandle[STATION_KEY] = station
        _station.value = station
        _stationInfoScreenState.value = StationInfoScreenState.clear()
    }

    fun showPreviouslyPlayed(showPreviouslyPlayed: Boolean) {
        _showPreviouslyPlayed.value = showPreviouslyPlayed
    }

    private fun user(user: UserState) {
        savedStateHandle[USER_KEY] = user
        _user.value = user
    }

    fun subscribeStationInfo(refresh: Boolean = false) {
        val station = _station.value ?: return

        _stationInfoScreenState.value = StationInfoScreenState.loading(
            _stationInfoScreenState.value)

        cancel(stationInfoJob)
        stationInfoJob = stationRepository.getStationInfoFlow(station.id, refresh)
            .autoRetry(connectivityObserver, coroutineDispatchers) { e ->
                _stationInfoScreenState.value =
                    StationInfoScreenState.error(e.toOperationError(infoErrorResponseConverter))
            }
            .combine(showPreviouslyPlayed) { infoResponse, showPreviouslyPlayed ->
                _stationInfoScreenState.value = StationInfoScreenState.loaded(
                    StationInfo(infoResponse, showPreviouslyPlayed, isTv))
                user(UserState(infoResponse.user))
            }
            .flowOn(coroutineDispatchers.compute)
            .launchWithDefaults(viewModelScope, "Station Info in Main VM")
    }

    fun unsubscribeStationInfo() {
        cancel(stationInfoJob)
    }

    fun faveSong(song: SongState) {
        uiEventDelegate.send(DismissSnackbarEvent)
        faveSongDelegate.faveSong(viewModelScope, song)
    }

    fun faveAlbum(album: AlbumState) {
        val station = _station.value ?: return
        uiEventDelegate.send(DismissSnackbarEvent)
        faveAlbumDelegate.faveAlbum(viewModelScope, station, album)
    }

    fun voteSong(eventId: Int, entryId: Int) {
        val station = _station.value ?: return
        uiEventDelegate.send(DismissSnackbarEvent)
        if (userRepository.isLoggedIn()) {
            voteSongDelegate.voteSong(viewModelScope, station.id, eventId, entryId)
        } else {
            uiEventDelegate.send(UserNotLoggedInEvent)
        }
    }

    fun rateSong(ratingState: RatingState) {
        val station = _station.value ?: return
        val items = _stationInfoScreenState.value.stationInfo?.items ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        val found = items.asSequence().filterIsInstance<StationInfoSongItem>()
            .firstOrNull { it.data.songId == ratingState.songId }
        if (found != null) {
            rateSong(station.id, found.data.song, ratingState.rating)
        } else {
            rateSong(station.id, ratingState.songId, ratingState.rating)
        }
    }

    fun rateSong(stationId: Int, song: SongState, newRating: Float) {
        rateSongDelegate.rateSong(viewModelScope, stationId, song, newRating)
    }

    private fun rateSong(stationId: Int, songId: Int, newRating: Float) {
        rateSongDelegate.rateSong(viewModelScope, stationId, songId, newRating, {
            uiEventDelegate.send(RateSongSuccessEvent(it))
        }, {
            uiEventDelegate.send(RateSongErrorEvent(it) { rateSong(stationId, songId, newRating )})
        })
    }

    fun subscribeStateChangeEvents() {
        cancel(faveSongStateJob)
        faveSongStateJob = faveSongDelegate.faveSongState
            .onEach { state ->
                if (state.success && state.song == null) {
                    _stationInfoScreenState.value.stationInfo?.items?.let { items ->
                        items.asSequence()
                            .filterIsInstance<StationInfoSongItem>()
                            .firstOrNull { it.data.songId == state.songId }?.let {
                                it.data.song.favorite.value = state.favorite
                            }
                    }
                } else if (state.error != null && state.song != null) {
                    uiEventDelegate.send(
                        FaveSongErrorEvent(songId = state.songId,
                            favorite = state.favorite,
                            error = state.error,
                            retry = { faveSong(state.song) })
                    )
                }
            }
            .flowOn(coroutineDispatchers.compute)
            .launchWithDefaults(viewModelScope, "Fave Song State Changed in Station Info")

        cancel(voteSongStateJob)
        voteSongStateJob = voteSongDelegate.voteSongState
            .onEach { state ->
                if (state.success) {
                    _stationInfoScreenState.value.stationInfo?.items?.let { items ->
                        items.asSequence()
                            .filterIsInstance<ComingUpSongItem>()
                            .filter { it.eventId == state.eventId }
                            .forEach { item ->
                                val song = item.data.song
                                song.voted.value = song.entryId == state.entryId
                            }
                    }
                } else if (state.error != null) {
                    uiEventDelegate.send(
                        VoteSongErrorEvent(
                            error = state.error,
                            retry = { voteSong(state.eventId, state.entryId) })
                    )
                }
            }
            .flowOn(coroutineDispatchers.compute)
            .launchWithDefaults(viewModelScope, "Vote Song State Changed in Station Info")

        cancel(rateSongStateJob)
        rateSongStateJob = rateSongDelegate.rateSongState
            .onEach { state ->
                if (state.success) {
                    // do nothing
                } else if (state.error != null) {
                    uiEventDelegate.send(
                        RateSongErrorEvent(
                            error = state.error,
                            retry = { rateSong(state.stationId, state.song, state.rating) }
                        )
                    )
                }
            }
            .flowOn(coroutineDispatchers.compute)
            .launchWithDefaults(viewModelScope, "Rate Song State Changed in Station Info")

        cancel(faveAlbumStateJob)
        faveAlbumStateJob = faveAlbumDelegate.faveAlbumState
            .onEach { state ->
                if (state.success) {
                    // do nothing
                } else if (state.error != null) {
                    uiEventDelegate.send(
                        FaveAlbumErrorEvent(
                            album = state.album,
                            error = state.error,
                            retry = { faveAlbum(state.album) }
                        )
                    )
                }
            }
            .flowOn(coroutineDispatchers.compute)
            .launchWithDefaults(viewModelScope, "Fave Album State Changed in Station Info")

        cancel(playbackStateJob)
        playbackStateJob = mediaPlayerStateObserver.flow
            .onEach {
                Timber.d(
                    "media player state changed, station = %s, state = %s",
                    it.station.name, it.state
                )
                _playbackState.value = it
            }
            .launchWithDefaults(viewModelScope, "Media Player State for in Station Info")

        cancel(refreshStationInfoJob)
        refreshStationInfoJob = uiEventDelegate.events.filterIsInstance<RefreshStationInfo>()
            .filter { it.stationId == station.value?.id }
            .onEach { subscribeStationInfo(refresh = true) }
            .launchWithDefaults(viewModelScope, "Refresh Station Info event in Station Info")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveSongStateJob, faveAlbumStateJob, voteSongStateJob, rateSongStateJob,
            playbackStateJob, refreshStationInfoJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(stationsJob, stationInfoJob, refreshStationInfoJob, faveSongStateJob, faveAlbumStateJob,
            voteSongStateJob, rateSongStateJob, playbackStateJob)
    }

    fun togglePlayback() {
        val station = _station.value ?: return
        playbackManager.togglePlayback(station)
    }

    fun showSnackbarMessage(message: String) {
        uiEventDelegate.sendSnackbarEvent(message)
    }

    fun getExistingSleepTimer(): Long? {
        return sleepTimerDelegate.getExistingSleepTimer()
    }

    fun createSleepTimer(hour: Int, minute: Int): Long {
        return sleepTimerDelegate.createSleepTimer(hour, minute)
    }

    fun removeSleepTimer() {
        sleepTimerDelegate.removeSleepTimer()
    }

    companion object {
        private const val STATION_KEY = "com.flashsphere.data.station"
        private const val USER_KEY = "com.flashsphere.data.user"
    }
}

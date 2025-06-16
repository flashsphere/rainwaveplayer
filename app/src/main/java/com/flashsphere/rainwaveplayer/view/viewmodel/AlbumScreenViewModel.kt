package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.model.request.RequestSongResponse
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.AlbumScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongSuccessEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import okhttp3.ResponseBody
import retrofit2.Converter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class AlbumScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
    private val faveSongDelegate: FaveSongDelegate,
    @Named("request_song_converter")
    private val requestSongResponseConverter: Converter<ResponseBody, RequestSongResponse>,
) : ViewModel() {
    val snackbarEvents = uiEventDelegate.snackbarEvents

    private var _station: Station? = savedStateHandle[STATION_KEY]
    val station get() = _station

    private val _albumScreenState = MutableStateFlow(AlbumScreenState())
    val albumScreenState = _albumScreenState.asStateFlow()

    private val requestSongDelegate = RequestSongDelegate(viewModelScope, stationRepository,
        requestSongResponseConverter)

    private var faveSongStateJob: Job? = null
    private var albumDetailJob: Job? = null

    fun getAlbum(station: Station, albumDetail: AlbumDetail) {
        if (_albumScreenState.value.loaded &&
            _albumScreenState.value.album?.id == albumDetail.id) return

        savedStateHandle[STATION_KEY] = station
        _station = station

        val album = AlbumState(albumDetail.id, albumDetail.name)
        _albumScreenState.value = AlbumScreenState.init(album)

        getAlbum()
    }

    fun getAlbum() {
        val station = _station ?: return
        val album = _albumScreenState.value.album ?: return

        _albumScreenState.value = AlbumScreenState.loading(_albumScreenState.value)
        uiEventDelegate.send(DismissSnackbarEvent)

        cancel(albumDetailJob)
        albumDetailJob = flow { emit(stationRepository.getAlbum(station.id, album.id)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _albumScreenState.value = AlbumScreenState.error(_albumScreenState.value,
                    OperationError(OperationError.Server))
            }
            .map { AlbumState(it.album) }
            .flowOn(coroutineDispatchers.compute)
            .onEach { _albumScreenState.value = AlbumScreenState.loaded(it) }
            .launchWithDefaults(viewModelScope, "Album Detail")
    }

    fun faveSong(song: SongState) {
        uiEventDelegate.send(DismissSnackbarEvent)
        faveSongDelegate.faveSong(viewModelScope, song)
    }

    fun requestSong(song: SongState) {
        val station = _station ?: return
        uiEventDelegate.send(DismissSnackbarEvent)
        requestSongDelegate.requestSong(station, song, {
            uiEventDelegate.send(RequestSongSuccessEvent(song = song))
        }, { error ->
            uiEventDelegate.send(RequestSongErrorEvent(song = song, error = error, retry = {
                requestSong(song)
            }))
        })
    }

    fun subscribeStateChangeEvents() {
        cancel(faveSongStateJob)
        faveSongStateJob = faveSongDelegate.faveSongState
            .onEach { state ->
                if (state.success && state.song == null) {
                    _albumScreenState.value.album?.songs?.let { songs ->
                        songs.firstOrNull { it.id == state.songId }?.let {
                            it.favorite.value = state.favorite
                        }
                    }
                } else if (state.error != null && state.song != null) {
                    uiEventDelegate.send(
                        FaveSongErrorEvent(
                            songId = state.songId,
                            favorite = state.favorite,
                            error = state.error,
                            retry = { faveSong(state.song) }
                        )
                    )
                }
            }
            .flowOn(coroutineDispatchers.compute)
            .launchWithDefaults(viewModelScope, "Fave Song State Changed in Album Detail")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveSongStateJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(albumDetailJob)
        cancel(faveSongStateJob)
    }

    companion object {
        private const val STATION_KEY = "com.flashsphere.data.station"
    }
}

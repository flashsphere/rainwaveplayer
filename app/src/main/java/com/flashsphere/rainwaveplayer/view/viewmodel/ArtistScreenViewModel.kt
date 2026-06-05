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
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.ArtistScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongSuccessEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class ArtistScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
    private val faveSongDelegate: FaveSongDelegate,
    @Named("request_song_converter")
    private val requestSongResponseConverter: Converter<ResponseBody, RequestSongResponse>,
) : ViewModel() {
    val snackbarEvents = uiEventDelegate.snackbarEvents

    private val stationFlow: MutableStateFlow<Station?> = savedStateHandle.getMutableStateFlow(
        STATION_KEY, null)

    val artistScreenState: StateFlow<ArtistScreenState>
        field = MutableStateFlow(ArtistScreenState())

    private val requestSongDelegate = RequestSongDelegate(viewModelScope, stationRepository,
        requestSongResponseConverter)

    private var faveSongStateJob: Job? = null
    private var artistDetailJob: Job? = null

    fun getArtist(station: Station, artistDetail: ArtistDetail) {
        if (artistScreenState.value.loaded &&
            artistScreenState.value.artist?.id == artistDetail.id) return

        stationFlow.value = station

        val artist = ArtistState(artistDetail.id, artistDetail.name)
        artistScreenState.value = ArtistScreenState.init(artist)

        getArtist()
    }

    fun getArtist() {
        val station = stationFlow.value ?: return
        val artist = artistScreenState.value.artist ?: return

        artistScreenState.value = ArtistScreenState.loading(artistScreenState.value)
        uiEventDelegate.send(DismissSnackbarEvent)

        cancel(artistDetailJob)
        artistDetailJob = flow { emit(stationRepository.getArtist(station.id, artist.id)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                artistScreenState.value = ArtistScreenState.error(artistScreenState.value,
                    OperationError(OperationError.Server))
            }
            .map { ArtistState(it.second, station, it.first) }
            .flowOn(coroutineDispatchers.compute)
            .onEach { artistScreenState.value = ArtistScreenState.loaded(it) }
            .launchWithDefaults(viewModelScope, "Artist Detail")
    }

    fun faveSong(song: SongState) {
        uiEventDelegate.send(DismissSnackbarEvent)
        faveSongDelegate.faveSong(viewModelScope, song)
    }

    fun requestSong(song: SongState) {
        val station = stationFlow.value ?: return
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
                if (state.success && state.song != null) {
                    artistScreenState.value.artist?.items?.let { items ->
                        items.asSequence().filterIsInstance<SongState>()
                            .firstOrNull { it.id == state.songId }?.let {
                                it.favorite.value = state.favorite
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
            .launchWithDefaults(viewModelScope, "Fave Song State Changed in Artist Detail")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveSongStateJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(artistDetailJob, faveSongStateJob)
    }

    companion object {
        private const val STATION_KEY = "com.flashsphere.data.station"
    }
}

package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.station.StationsErrorResponse
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.view.uistate.StationsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import okhttp3.ResponseBody
import retrofit2.Converter
import timber.log.Timber

@HiltViewModel
class UserPagedListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
    private val faveSongDelegate: FaveSongDelegate,
    @Named("stations_error_response_converter")
    private val stationsErrorResponseConverter: Converter<ResponseBody, StationsErrorResponse>,
) : ViewModel() {
    val snackbarEvents = uiEventDelegate.snackbarEvents

    val stationsScreenState: StateFlow<StationsScreenState>
        field = MutableStateFlow(StationsScreenState())

    val station: StateFlow<Station?>
        field = savedStateHandle.getMutableStateFlow(STATION_KEY, null)

    private val updatedItems = MutableStateFlow(mapOf<Int, Boolean>())

    private var stationsJob: Job? = null

    private var faveSongStateJob: Job? = null

    fun station(station: Station) {
        this.station.value = station
    }

    fun getStations() {
        stationsScreenState.value = StationsScreenState.loading()

        cancel(stationsJob)
        stationsJob = flow { emit(stationRepository.getStations()) }
            .autoRetry(connectivityObserver, coroutineDispatchers) { e ->
                stationsScreenState.value = StationsScreenState.error(
                    e.toOperationError(stationsErrorResponseConverter))
            }
            .onEach { stations ->
                stationsScreenState.value = StationsScreenState.loaded(stations)
                if (station.value == null) {
                    station(stations[0])
                }
            }
            .launchWithDefaults(viewModelScope, "Stations in Paged List")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allFaves: Flow<PagingData<SongState>> = this.station.filterNotNull()
        .flatMapLatest { station ->
            stationRepository.allFaves(station.id, PagingConfig(pageSize = 30, enablePlaceholders = false))
                .flow
                .map { pagingData -> pagingData.map { song -> SongState(song) } }
                .cachedIn(viewModelScope)
                .combine(updatedItems) { pagingData, updated ->
                    pagingData.map { song ->
                        song.also {
                            it.favorite.value = updated.getOrDefault(it.id, it.favorite.value)
                        }
                    }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentVotes: Flow<PagingData<SongState>> = this.station.filterNotNull()
        .flatMapLatest { station ->
            stationRepository.recentVotes(station.id, PagingConfig(pageSize = 30, enablePlaceholders = false))
                .flow
                .map { pagingData -> pagingData.map { song -> SongState(song) } }
                .cachedIn(viewModelScope)
                .combine(updatedItems) { pagingData, updated ->
                    pagingData.map { song ->
                        song.also {
                            it.favorite.value = updated.getOrDefault(it.id, it.favorite.value)
                        }
                    }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val requestHistory: Flow<PagingData<SongState>> = this.station.filterNotNull()
        .flatMapLatest { station ->
            stationRepository.requestHistory(station.id, PagingConfig(pageSize = 30, enablePlaceholders = false))
                .flow
                .map { pagingData -> pagingData.map { song -> SongState(song) } }
                .cachedIn(viewModelScope)
                .combine(updatedItems) { pagingData, updated ->
                    pagingData.map { song ->
                        song.also {
                            it.favorite.value = updated.getOrDefault(it.id, it.favorite.value)
                        }
                    }
                }
        }

    fun faveSong(song: SongState) = faveSongDelegate.faveSong(viewModelScope, song)

    fun subscribeStateChangeEvents() {
        cancel(faveSongStateJob)
        faveSongStateJob = faveSongDelegate.faveSongState
            .onEach { state ->
                if (state.success && state.song == null) {
                    updatedItems.emit(mapOf(state.songId to state.favorite))
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
            .launchWithDefaults(viewModelScope, "Fave Song State Changed in Paged List")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveSongStateJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(stationsJob, faveSongStateJob)
    }

    companion object {
        private const val STATION_KEY = "com.flashsphere.data.station"
    }
}

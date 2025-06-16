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
import com.flashsphere.rainwaveplayer.ui.navigation.CategoryDetail
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.CategoryScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongSuccessEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState
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
class CategoryScreenViewModel @Inject constructor(
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

    private val _categoryScreenState = MutableStateFlow(CategoryScreenState())
    val categoryScreenState = _categoryScreenState.asStateFlow()

    private val requestSongDelegate = RequestSongDelegate(viewModelScope, stationRepository,
        requestSongResponseConverter)

    private var faveSongStateJob: Job? = null
    private var categoryJob: Job? = null

    fun getCategory(station: Station, categoryDetail: CategoryDetail) {
        if (_categoryScreenState.value.loaded &&
            _categoryScreenState.value.category?.id == categoryDetail.id) return

        savedStateHandle[STATION_KEY] = station
        _station = station

        val category = CategoryState(categoryDetail.id, categoryDetail.name)
        _categoryScreenState.value = CategoryScreenState.init(category)

        getCategory()
    }

    fun getCategory() {
        val station = _station ?: return
        val category = _categoryScreenState.value.category ?: return

        _categoryScreenState.value = CategoryScreenState.loading(_categoryScreenState.value)
        uiEventDelegate.send(DismissSnackbarEvent)

        cancel(categoryJob)
        categoryJob = flow { emit(stationRepository.getCategory(station.id, category.id)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _categoryScreenState.value = CategoryScreenState.error(
                    _categoryScreenState.value, OperationError(OperationError.Server)
                )
            }
            .map { CategoryState(it.category) }
            .flowOn(coroutineDispatchers.compute)
            .onEach { _categoryScreenState.value = CategoryScreenState.loaded(it) }
            .launchWithDefaults(viewModelScope, "Category Detail")
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
                    _categoryScreenState.value.category?.items?.let { items ->
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
            .launchWithDefaults(viewModelScope, "Fave Song State Changed in Category Detail")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveSongStateJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(categoryJob, faveSongStateJob)
    }

    companion object {
        private const val STATION_KEY = "com.flashsphere.data.station"
    }
}

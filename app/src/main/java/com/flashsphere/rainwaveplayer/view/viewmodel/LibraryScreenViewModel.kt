package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.LibraryScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveAlbumErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState
import com.flashsphere.rainwaveplayer.view.uistate.model.LibraryItem
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestLineState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.LITERAL
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
    private val faveAlbumDelegate: FaveAlbumDelegate,
) : ViewModel() {
    val snackbarEvents = uiEventDelegate.snackbarEvents

    val filterTextFieldState = TextFieldState(savedStateHandle[FILTER_QUERY_KEY] ?: "")
    private val _filterQueryEvent = MutableStateFlow(FilterQueryEvent("", false))
    @OptIn(FlowPreview::class)
    private val debounceFilterFlow = _filterQueryEvent
        .debounce { (queryText, isSubmitted) ->
            if (isSubmitted || queryText.isEmpty()) {
                0.milliseconds
            } else {
                500.milliseconds
            }
        }
        .distinctUntilChanged { old, new -> old.queryText == new.queryText }
        .map { it.queryText }

    private val _albumLibraryState = MutableStateFlow(LibraryScreenState<AlbumState>())
    val albumLibraryState = _albumLibraryState.asStateFlow()

    private val _artistLibraryState = MutableStateFlow(LibraryScreenState<ArtistState>())
    val artistLibraryState = _artistLibraryState.asStateFlow()

    private val _categoryLibraryState = MutableStateFlow(LibraryScreenState<CategoryState>())
    val categoryLibraryState = _categoryLibraryState.asStateFlow()

    private val _requestLineLibraryState = MutableStateFlow(LibraryScreenState<RequestLineState>())
    val requestLineLibraryState = _requestLineLibraryState.asStateFlow()

    private var allAlbumsJob: Job? = null
    private var allArtistsJob: Job? = null
    private var allCategoriesJob: Job? = null
    private var requestLineJob: Job? = null
    private var faveAlbumStateJob: Job? = null

    init {
        viewModelScope.launch {
            snapshotFlow { filterTextFieldState.text }
                .collectLatest { filterQuery(it.toString()) }
        }
    }

    fun resetFilter() {
        filterTextFieldState.clearText()
        submitFilter()
    }

    fun submitFilter() {
        filterQuery(filterTextFieldState.text.toString(), true)
    }

    private fun filterQuery(filterQuery: String, submitted: Boolean = false) {
        savedStateHandle[FILTER_QUERY_KEY] = filterQuery
        _filterQueryEvent.value = FilterQueryEvent(filterQuery.trim(), submitted)
    }

    fun getAllAlbums(station: Station, force: Boolean = false): Job {
        if (force || _albumLibraryState.value.station != station) {
            _albumLibraryState.value = LibraryScreenState.loading(_albumLibraryState.value, station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(allAlbumsJob)
        return flow { emit(stationRepository.getAllAlbums(station.id, force)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _albumLibraryState.value = LibraryScreenState.error(_albumLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.albums.asSequence()
                    .map { item -> AlbumState(item) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { _albumLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "All Albums")
            .also { allAlbumsJob = it }
    }

    fun getAllArtists(station: Station, force: Boolean = false): Job {
        if (force || _artistLibraryState.value.station != station) {
            _artistLibraryState.value = LibraryScreenState.loading(_artistLibraryState.value, station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(allArtistsJob)
        return flow { emit(stationRepository.getAllArtists(station.id, force)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _artistLibraryState.value = LibraryScreenState.error(_artistLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.artists.asSequence()
                    .map { item -> ArtistState(item.id, item.name) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { _artistLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "All Artists")
            .also { allArtistsJob = it }
    }

    fun getAllCategories(station: Station, force: Boolean = false): Job {
        if (force || _categoryLibraryState.value.station != station) {
            _categoryLibraryState.value = LibraryScreenState.loading(_categoryLibraryState.value,
                station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(allCategoriesJob)
        return flow { emit(stationRepository.getAllCategories(station.id, force)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _categoryLibraryState.value = LibraryScreenState.error(_categoryLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.categories.asSequence()
                    .map { item -> CategoryState(item.id, item.name) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { _categoryLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "All Categories")
            .also { allCategoriesJob = it }
    }

    fun subscribeStationInfo(station: Station, force: Boolean = false): Job {
        if (force || _requestLineLibraryState.value.station != station) {
            _requestLineLibraryState.value = LibraryScreenState.loading(
                _requestLineLibraryState.value, station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(requestLineJob)
        return stationRepository.getStationInfoFlow(station.id, force)
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _requestLineLibraryState.value = LibraryScreenState.error(_requestLineLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                if (force) {
                    // workaround Pull To Refresh indicator not disappearing when force refresh
                    // because the API returns the response too quickly
                    delay(100)
                }
                val data = response.requestLine.asSequence()
                    .map { item -> RequestLineState(item) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { _requestLineLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "Request Line")
            .also { requestLineJob = it }
    }

    fun unsubscribeStationInfo() {
        cancel(requestLineJob)
    }

    fun faveAlbum(station: Station, album: AlbumState) {
        uiEventDelegate.send(DismissSnackbarEvent)
        faveAlbumDelegate.faveAlbum(viewModelScope, station, album)
    }

    fun subscribeStateChangeEvents() {
        faveAlbumStateJob = faveAlbumDelegate.faveAlbumState
            .onEach {
                if (it.success) {
                    // do nothing
                } else if (it.error != null) {
                    uiEventDelegate.send(
                        FaveAlbumErrorEvent(
                            album = it.album,
                            error = it.error,
                            retry = { faveAlbum(it.station, it.album) }
                        )
                    )
                }
            }
            .launchWithDefaults(viewModelScope, "Fave Album State Changed in Library")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveAlbumStateJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(allAlbumsJob, allArtistsJob, allCategoriesJob, requestLineJob, faveAlbumStateJob)
    }

    private data class FilterQueryEvent(
        val queryText: String,
        val isSubmitted: Boolean,
    )

    companion object {
        private const val FILTER_QUERY_KEY = "com.flashsphere.data.filter_query"

        private fun <T : LibraryItem> filterData(query: String, data: SnapshotStateList<T>): SnapshotStateList<T> {
            if (query.isBlank()) return data

            val regex = query.toRegex(setOf(IGNORE_CASE, LITERAL))
            return data.asSequence()
                .filter { it.searchable.isNotEmpty() && regex.containsMatchIn(it.searchable) }
                .toCollection(mutableStateListOf())
        }
    }
}

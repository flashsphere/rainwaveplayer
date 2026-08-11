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
import jakarta.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val albumLibraryState: StateFlow<LibraryScreenState<AlbumState>>
        field = MutableStateFlow(LibraryScreenState())

    val artistLibraryState: StateFlow<LibraryScreenState<ArtistState>>
        field = MutableStateFlow(LibraryScreenState())

    val categoryLibraryState: StateFlow<LibraryScreenState<CategoryState>>
        field = MutableStateFlow(LibraryScreenState())

    val requestLineLibraryState: StateFlow<LibraryScreenState<RequestLineState>>
        field = MutableStateFlow(LibraryScreenState())

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
        if (force || albumLibraryState.value.station != station) {
            albumLibraryState.value = LibraryScreenState.loading(albumLibraryState.value, station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(allAlbumsJob)
        return flow { emit(stationRepository.getAllAlbums(station.id, force)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                albumLibraryState.value = LibraryScreenState.error(albumLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.albums.asSequence()
                    .map { item -> AlbumState(item) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { albumLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "All Albums")
            .also { allAlbumsJob = it }
    }

    fun getAllArtists(station: Station, force: Boolean = false): Job {
        if (force || artistLibraryState.value.station != station) {
            artistLibraryState.value = LibraryScreenState.loading(artistLibraryState.value, station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(allArtistsJob)
        return flow { emit(stationRepository.getAllArtists(station.id, force)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                artistLibraryState.value = LibraryScreenState.error(artistLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.artists.asSequence()
                    .map { item -> ArtistState(item.id, item.name) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { artistLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "All Artists")
            .also { allArtistsJob = it }
    }

    fun getAllCategories(station: Station, force: Boolean = false): Job {
        if (force || categoryLibraryState.value.station != station) {
            categoryLibraryState.value = LibraryScreenState.loading(categoryLibraryState.value,
                station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(allCategoriesJob)
        return flow { emit(stationRepository.getAllCategories(station.id, force)) }
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                categoryLibraryState.value = LibraryScreenState.error(categoryLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.categories.asSequence()
                    .map { item -> CategoryState(item.id, item.name) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { categoryLibraryState.value = it }
            .launchWithDefaults(viewModelScope, "All Categories")
            .also { allCategoriesJob = it }
    }

    fun subscribeStationInfo(station: Station, force: Boolean = false): Job {
        if (force || requestLineLibraryState.value.station != station) {
            requestLineLibraryState.value = LibraryScreenState.loading(
                requestLineLibraryState.value, station)
            uiEventDelegate.send(DismissSnackbarEvent)
        }

        cancel(requestLineJob)
        return stationRepository.getStationInfoFlow(station.id, force)
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                requestLineLibraryState.value = LibraryScreenState.error(requestLineLibraryState.value,
                    OperationError(OperationError.Server))
            }
            .combine(debounceFilterFlow) { response, filterQuery ->
                val data = response.requestLine.asSequence()
                    .map { item -> RequestLineState(item) }
                    .toCollection(mutableStateListOf())
                LibraryScreenState.loaded(station, data, filterData(filterQuery, data))
            }
            .flowOn(coroutineDispatchers.compute)
            .onEach { requestLineLibraryState.value = it }
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

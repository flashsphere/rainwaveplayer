package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.model.request.RequestSongResponse
import com.flashsphere.rainwaveplayer.model.search.SearchResponse
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.view.uistate.SearchScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveAlbumErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.FaveSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestSongSuccessEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumSearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistSearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchResult
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongSearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Converter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
    private val faveSongDelegate: FaveSongDelegate,
    private val faveAlbumDelegate: FaveAlbumDelegate,
    @Named("search_converter")
    private val searchResponseConverter: Converter<ResponseBody, SearchResponse>,
    @Named("request_song_converter")
    private val requestSongResponseConverter: Converter<ResponseBody, RequestSongResponse>,
) : ViewModel() {
    val snackbarEvents = uiEventDelegate.snackbarEvents

    private val requestSongDelegate = RequestSongDelegate(viewModelScope, stationRepository,
        requestSongResponseConverter)

    private var station: Station? = null
    val searchTextFieldState = TextFieldState(savedStateHandle[SEARCH_QUERY_KEY] ?: "")

    private val _searchState = MutableStateFlow(SearchScreenState())
    val searchState = _searchState.asStateFlow()

    private var faveSongStateJob: Job? = null
    private var faveAlbumStateJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            snapshotFlow { searchTextFieldState.text }
                .collectLatest { savedStateHandle[SEARCH_QUERY_KEY] = it }
        }
    }

    fun station(station: Station) {
        if (this.station == null) {
            search(station)
        }
        this.station = station
    }

    fun search() {
        val station = station ?: return
        search(station)
    }

    private fun search(station: Station) {
        val query = searchTextFieldState.text.trim().toString()
        if (query.isEmpty()) {
            return
        }

        _searchState.value = SearchScreenState.loading()

        cancel(searchJob)
        searchJob = flow { emit(stationRepository.search(station.id, query)) }
            .map { response ->
                if (response.result == null) {
                    val items = mutableStateListOf<SearchItem>()

                    if (response.artists.isNotEmpty()) {
                        items.add(ArtistSearchHeaderItem)
                        response.artists.forEach { items.add(ArtistState(it.id, it.name)) }
                    }
                    if (response.albums.isNotEmpty()) {
                        items.add(AlbumSearchHeaderItem)
                        response.albums.forEach { items.add(AlbumState(it)) }
                    }
                    if (response.songs.isNotEmpty()) {
                        items.add(SongSearchHeaderItem)
                        response.songs.forEach { items.add(SearchSongItem(it)) }
                    }
                    SearchResult(items = items)
                } else {
                    SearchResult(message = response.result.text)
                }
            }
            .flowOn(coroutineDispatchers.compute)
            .autoRetry(connectivityObserver, coroutineDispatchers) { e ->
                _searchState.value = SearchScreenState.error(
                    e.toOperationError(searchResponseConverter))
            }
            .onEach { _searchState.value = SearchScreenState.loaded(it) }
            .launchWithDefaults(viewModelScope, "Search Results")
    }

    fun faveAlbum(album: AlbumState) {
        val station = station ?: return
        uiEventDelegate.send(DismissSnackbarEvent)
        faveAlbumDelegate.faveAlbum(viewModelScope, station, album)
    }

    fun faveSong(song: SongState) {
        uiEventDelegate.send(DismissSnackbarEvent)
        faveSongDelegate.faveSong(viewModelScope, song)
    }

    fun requestSong(song: SongState) {
        val station = station ?: return
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
                    _searchState.value.result?.items?.let { items ->
                        items.asSequence()
                            .filterIsInstance<SearchSongItem>()
                            .firstOrNull { it.song.id == state.songId }?.let {
                                it.song.favorite.value = state.favorite
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
            .launchWithDefaults(viewModelScope, "Fave Song State Changed in Search")

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
            .launchWithDefaults(viewModelScope, "Fave Album State Changed in Search")
    }

    fun unsubscribeStateChangeEvents() {
        cancel(faveSongStateJob, faveAlbumStateJob)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(faveSongStateJob, faveAlbumStateJob, searchJob)
    }

    companion object {
        private const val SEARCH_QUERY_KEY = "com.flashsphere.data.search_query"
    }
}

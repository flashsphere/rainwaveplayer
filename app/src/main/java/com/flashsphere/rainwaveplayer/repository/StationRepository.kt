package com.flashsphere.rainwaveplayer.repository

import android.content.Context
import androidx.collection.SieveCache
import androidx.collection.mutableScatterMapOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.flow.StationInfoProvider
import com.flashsphere.rainwaveplayer.internal.datastore.SavedStationsStore
import com.flashsphere.rainwaveplayer.model.ApiResponse
import com.flashsphere.rainwaveplayer.model.FailureApiResponse
import com.flashsphere.rainwaveplayer.model.SuccessApiResponse
import com.flashsphere.rainwaveplayer.model.album.AlbumResponse
import com.flashsphere.rainwaveplayer.model.album.AllAlbums
import com.flashsphere.rainwaveplayer.model.artist.AllArtists
import com.flashsphere.rainwaveplayer.model.artist.Artist
import com.flashsphere.rainwaveplayer.model.category.AllCategories
import com.flashsphere.rainwaveplayer.model.category.CategoryResponse
import com.flashsphere.rainwaveplayer.model.favorite.FaveAlbumResponse
import com.flashsphere.rainwaveplayer.model.favorite.FaveSongResponse
import com.flashsphere.rainwaveplayer.model.request.ClearRequestsResponse
import com.flashsphere.rainwaveplayer.model.request.DeleteRequestResponse
import com.flashsphere.rainwaveplayer.model.request.OrderRequestsResponse
import com.flashsphere.rainwaveplayer.model.request.PauseRequestResponse
import com.flashsphere.rainwaveplayer.model.request.Request
import com.flashsphere.rainwaveplayer.model.request.RequestFaveResponse
import com.flashsphere.rainwaveplayer.model.request.RequestSongResponse
import com.flashsphere.rainwaveplayer.model.request.RequestUnratedResponse
import com.flashsphere.rainwaveplayer.model.request.ResumeRequestResponse
import com.flashsphere.rainwaveplayer.model.search.SearchResponse
import com.flashsphere.rainwaveplayer.model.song.RateSongResponse
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.model.user.User
import com.flashsphere.rainwaveplayer.model.vote.VoteResponse
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.ADD_REQUEST_TO_TOP
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_CLEAR
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_FAVE
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_UNRATED
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.LAST_PLAYED
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.SSL_RELAY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.USE_OGG
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.updateBlocking
import com.flashsphere.rainwaveplayer.view.paging.AllFavesPagingSource
import com.flashsphere.rainwaveplayer.view.paging.RecentVotesPagingSource
import com.flashsphere.rainwaveplayer.view.paging.RequestHistoryPagingSource
import com.flashsphere.rainwaveplayer.view.uistate.event.RefreshStationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class StationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rainwaveService: RainwaveService,
    private val dataStore: DataStore<Preferences>,
    private val savedStationsStore: SavedStationsStore,
    private val userRepository: UserRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
) {
    private var stationsCache: List<Station>? = null

    private val stationInfoProviderCache = SieveCache<Int, StationInfoProvider>(5)
    private val allAlbumCache = mutableScatterMapOf<Int, AllAlbums>()
    private val allArtistsCache = mutableScatterMapOf<Int, AllArtists>()
    private val categoriesCache = mutableScatterMapOf<Int, AllCategories>()

    suspend fun clearCache() {
        Timber.d("clearCache")
        stationsCache = null
        savedStationsStore.remove()
        stationInfoProviderCache.evictAll()
        allAlbumCache.clear()
        allArtistsCache.clear()
        categoriesCache.clear()
    }

    fun cleanupCache() {
        val stationIdsToRemove = hashSetOf<Int>()
        stationInfoProviderCache.forEach { key, value ->
            if (!value.hasObservers()) {
                stationIdsToRemove += key
            }
        }

        // keep the most recently used station if there are 0 existing observers
        if (stationIdsToRemove.size == stationInfoProviderCache.count) {
            stationInfoProviderCache.trimToSize(1)

            val iterator = stationIdsToRemove.iterator()
            while (iterator.hasNext()) {
                if (stationInfoProviderCache.contains(iterator.next())) {
                    iterator.remove()
                }
            }
        }

        Timber.d("Removing cache for station ids: %s", stationIdsToRemove)

        allAlbumCache.removeIf { i, _ -> stationIdsToRemove.contains(i) }
        allArtistsCache.removeIf { i, _ -> stationIdsToRemove.contains(i) }
        categoriesCache.removeIf { i, _ -> stationIdsToRemove.contains(i) }

        Timber.d("stationInfoFlowCache.count = %d", stationInfoProviderCache.count)
        Timber.d("allAlbumCache.size = %d", allAlbumCache.size)
        Timber.d("allArtistsCache.size = %d", allArtistsCache.size)
        Timber.d("categoriesCache.size = %d", categoriesCache.size)
    }

    private suspend fun fetchStationsFromApi(): List<Station> = withContext(coroutineDispatchers.network) {
        val resources = context.resources
        rainwaveService.fetchStations().stations.map { s ->
            val name = when (s.id) {
                // translate station name to respective language
                1 -> resources.getString(R.string.station_name_1)
                2 -> resources.getString(R.string.station_name_2)
                3 -> resources.getString(R.string.station_name_3)
                4 -> resources.getString(R.string.station_name_4)
                5 -> resources.getString(R.string.station_name_5)
                else -> s.name
            }
            Timber.d("%s: %s, %s", name, s.stream, s.relays)
            Station(s.id, name, s.stream, s.description, s.relays)
        }
    }

    private suspend fun fetchStationsFromLocalCache(): List<Station>? {
        val savedStations = suspendRunCatching { savedStationsStore.get() }.getOrNull() ?: return null
        val timeElapsedHours = (System.currentTimeMillis() - savedStations.timestamp)
            .milliseconds.inWholeHours
        if (timeElapsedHours >= 24) {
            // refresh cache with API
            coroutineDispatchers.scope.launchWithDefaults("Update stations local cache") {
                Timber.d("Refreshing local cache with API")
                suspendRunCatching { fetchStationsFromApi() }
                    .onSuccess { savedStationsStore.save(it) }
            }
        }
        return savedStations.stations
    }

    private val stationsFlow =
        flow {
            val stations = stationsCache
            if (!stations.isNullOrEmpty()) {
                Timber.d("Got stations from in-mem cache")
                emit(ApiResponse.ofSuccess(stations))
            }
        }.onEmpty {
            val stations = fetchStationsFromLocalCache()
            if (!stations.isNullOrEmpty()) {
                Timber.d("Got stations from local cache")
                stationsCache = stations
                emit(ApiResponse.ofSuccess(stations))
            }
        }.onEmpty {
            // fetch from network if empty
            val stations = fetchStationsFromApi()
            Timber.d("Got stations from API")
            savedStationsStore.save(stations)
            stationsCache = stations
            emit(ApiResponse.ofSuccess(stations))
        }
        .catch { emit(ApiResponse.ofFailure(it)) }
        .shareIn(coroutineDispatchers.scope, WhileSubscribed(replayExpirationMillis = 0))

    fun hasLocalCachedStations(): Boolean = runBlocking { savedStationsStore.exists() }

    suspend fun getStations(): List<Station> = stationsFlow
        .map { apiResponse ->
            when (apiResponse) {
                is SuccessApiResponse -> apiResponse.data
                is FailureApiResponse -> throw apiResponse.exception
            }
        }
        .first()

    fun getRelayUrl(station: Station): String {
        val streamUrl = if (dataStore.getBlocking(SSL_RELAY)) {
            station.relays[0]
        } else station.stream

        return if (dataStore.getBlocking(USE_OGG) && streamUrl.contains(".mp3")) {
            streamUrl.replace(".mp3", ".ogg")
        } else {
            streamUrl
        }
    }

    private suspend fun getStationsWithLastPlayed(currentStation: Station?): Pair<List<Station>, Station> =
        withContext(coroutineDispatchers.network) {
            Pair(getStations(), currentStation ?: getLastPlayedStationWithDefault())
        }

    suspend fun getLastPlayedStationWithDefault(): Station = withContext(coroutineDispatchers.network) {
        Timber.d("getLastPlayedStationWithDefault")
        getLastPlayedStationWithoutDefault().let {
            if (it == null) {
                Timber.d("no last played found. returning first station in list")
                getStations()[0]
            } else {
                it
            }
        }
    }

    suspend fun getLastPlayedStationWithoutDefault(): Station? = withContext(coroutineDispatchers.network) {
        Timber.d("getLastPlayedStationWithoutDefault")
        val stationId = dataStore.get(LAST_PLAYED)

        if (stationId == -1) {
            null
        } else {
            getStations().find { station -> station.id == stationId }
                .also {
                    if (it != null) {
                        Timber.d("last played station: %s %s", it.name, it.stream)
                    }
                }
        }
    }

    suspend fun getPrevStation(currentStation: Station? = null): Station =
        withContext(coroutineDispatchers.network) {
            val (stations, station) = getStationsWithLastPlayed(currentStation)
            var prevIndex = stations.indexOf(station) - 1
            if (prevIndex < 0) {
                prevIndex = stations.size - 1
            }
            Timber.d("getPrevStation() index = %d", prevIndex)
            stations[prevIndex]
        }

    suspend fun getNextStation(currentStation: Station? = null): Station =
        withContext(coroutineDispatchers.network) {
            val (stations, station) = getStationsWithLastPlayed(currentStation)
            var nextIndex = stations.indexOf(station) + 1
            if (nextIndex >= stations.size) {
                nextIndex = 0
            }
            Timber.d("getNextStation() index = %d", nextIndex)
            stations[nextIndex]
        }

    suspend fun getStationWithQuery(query: String?): Station =
        withContext(coroutineDispatchers.network){
            if (query.isNullOrBlank()) {
                getLastPlayedStationWithDefault()
            } else {
                val strippedQuery = query.replace("\\s+".toRegex(), "").lowercase()
                Timber.d("strippedQuery = %s", strippedQuery)

                val matched = getStations().find {
                    strippedQuery.contains(it.name.lowercase().replace("\\s+".toRegex(), ""))
                }
                if (matched != null) {
                    matched
                } else {
                    Timber.i("No appropriate station found for query: '$query'")
                    getLastPlayedStationWithDefault()
                }
            }
        }

    suspend fun getStationWithId(id: String?): Station =
        withContext(coroutineDispatchers.network) {
            if (id.isNullOrBlank()) {
                getLastPlayedStationWithDefault()
            } else {
                val matched = getStations().find { id.toIntOrNull() == it.id }
                if (matched != null) {
                    matched
                } else {
                    Timber.i("No appropriate station found for id: '${id}'")
                    getLastPlayedStationWithDefault()
                }
            }
        }

    fun getCurrentStationInfo(stationId: Int): InfoResponse? {
        return stationInfoProviderCache[stationId]?.getCachedInfoResponse()
    }


    fun getStationInfoFlow(stationId: Int, refresh: Boolean = false): Flow<InfoResponse> {
        Timber.d("getStationInfoFlow stationId = %d, refresh = %s", stationId, refresh)
        var provider = stationInfoProviderCache[stationId]
        if (provider == null) {
            provider = StationInfoProvider(rainwaveService, userRepository, coroutineDispatchers, stationId)
            stationInfoProviderCache.put(stationId, provider)
        }

        if (refresh) {
            provider.refreshAtNextSubscription()
        }

        return provider.flow
            .onEach { populateStationName(it.requests) }
            .flowOn(coroutineDispatchers.compute)
    }

    fun refreshStationInfo(stationId: Int, delayInSeconds: Int = 0) {
        Timber.d("refreshStationInfo stationId = %d, delay = %d", stationId, delayInSeconds)
        stationInfoProviderCache[stationId]?.refreshAtNextSubscription(delayInSeconds) {
            uiEventDelegate.send(RefreshStationInfo(stationId))
        }
    }

    suspend fun vote(stationId: Int, entryId: Int): VoteResponse = withContext(coroutineDispatchers.network) {
        rainwaveService.vote(stationId, entryId).also { response ->
            if (response.result.success) {
                getCurrentStationInfo(stationId)?.futureEvents?.let { events ->
                    events.asSequence()
                        .filter { it.id == response.result.eventId }
                        .flatMap { it.songs }
                        .forEach { it.voted = it.entryId == response.result.entryId }
                }
            }
        }
    }

    suspend fun getSuggestedStations(): List<Station> = withContext(coroutineDispatchers.network) {
        Timber.d("getSuggestedStations")

        val stationId = dataStore.getBlocking(LAST_PLAYED)
        Timber.d("station id = %d", stationId)
        if (stationId == -1) {
            getStations()
        } else {
            val stations = getStations()
            val index = stations.indexOfFirst { station -> station.id == stationId }
            if (index == -1) {
                stations
            } else {
                Timber.d("moving last played station id %d to top", stationId)
                stations.toMutableList().apply {
                    val station = removeAt(index)
                    add(0, station)
                }
            }
        }
    }

    fun saveLastPlayedStation(station: Station) {
        Timber.d("saveLastPlayedStation")
        dataStore.updateBlocking(LAST_PLAYED, station.id)
    }

    suspend fun getAllAlbums(stationId: Int, refresh: Boolean): AllAlbums {
        val value = allAlbumCache[stationId]
        return if (value == null || refresh) {
            withContext(coroutineDispatchers.network) {
                rainwaveService.fetchAllAlbums(stationId).also {
                    allAlbumCache[stationId] = it
                }
            }
        } else {
            value
        }
    }

    suspend fun getAllArtists(stationId: Int, refresh: Boolean): AllArtists {
        val value = allArtistsCache[stationId]
        return if (value == null || refresh) {
            withContext(coroutineDispatchers.network) {
                rainwaveService.fetchAllArtists(stationId).also {
                    allArtistsCache[stationId] = it
                }
            }
        } else {
            value
        }
    }

    suspend fun getAllCategories(stationId: Int, refresh: Boolean): AllCategories {
        val value = categoriesCache[stationId]
        return if (value == null || refresh) {
            withContext(coroutineDispatchers.network) {
                rainwaveService.fetchAllGroups(stationId).also {
                    categoriesCache[stationId] = it
                }
            }
        } else {
            value
        }
    }

    suspend fun getAlbum(stationId: Int, albumId: Int): AlbumResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.fetchAlbum(stationId, albumId)
        }

    suspend fun getArtist(stationId: Int, artistId: Int): Pair<Map<Int, Station>, Artist> =
        withContext(coroutineDispatchers.network) {
            val deferredArtist = async { rainwaveService.fetchArtist(stationId, artistId).artist }
            val deferredStations = async { getStations() }

            val artist = deferredArtist.await()
            val stations = deferredStations.await().associateBy { it.id }
            Pair(stations, artist)
        }

    suspend fun getCategory(stationId: Int, categoryId: Int): CategoryResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.fetchGroup(stationId, categoryId)
        }

    suspend fun favoriteAlbum(stationId: Int, albumId: Int, fave: Boolean): FaveAlbumResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.favoriteAlbum(stationId, albumId, fave).also { response ->
                if (response.result.success) {
                    getCurrentStationInfo(stationId)?.albumsMap?.let { albums ->
                        albums[albumId]?.favorite = response.result.favorite
                    }

                    allAlbumCache[stationId]?.let { allAlbums ->
                        allAlbums.albumMap[albumId]?.favorite = response.result.favorite
                    }
                }
            }
        }

    suspend fun favoriteSong(songId: Int, fave: Boolean): FaveSongResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.favoriteSong(songId, fave).also { response ->
                if (response.result.success) {
                    stationInfoProviderCache.forEach { _, v ->
                        v.getCachedInfoResponse()?.let {
                            it.songsMap[songId]?.favorite = response.result.favorite
                        }
                    }
                }
            }
        }

    suspend fun requestSong(stationId: Int, songId: Int): RequestSongResponse =
        withContext(coroutineDispatchers.network) {
            val requestSongResponse = rainwaveService.requestSong(stationId, songId)
            if (requestSongResponse.result.success) {
                updateRequests(stationId, requestSongResponse.requests)
            }

            val addRequestToTopEnabled = dataStore.get(ADD_REQUEST_TO_TOP)
            if (addRequestToTopEnabled && requestSongResponse.result.success && requestSongResponse.requests.size > 1) {
                val requests = requestSongResponse.requests.toMutableList()
                val requestedSongIndex = requests.indexOfFirst { it.songId == songId }
                if (requestedSongIndex != -1) {
                    val requestedSong = requests.removeAt(requestedSongIndex)
                    requests.add(0, requestedSong)
                    orderRequests(stationId, requests)
                }
            }

            requestSongResponse
        }

    private suspend fun orderRequests(stationId: Int, requests: List<Request>): OrderRequestsResponse {
        val order = requests.joinToString(",") { it.songId.toString() }
        return orderRequests(stationId, order)
    }

    suspend fun orderRequests(stationId: Int, order: String): OrderRequestsResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.orderRequests(stationId, order).also {
                updateRequests(stationId, it.requests)
            }
        }

    suspend fun deleteRequest(stationId: Int, songId: Int): DeleteRequestResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.deleteRequest(stationId, songId).also {
                if (it.result.success) {
                    updateRequests(stationId, it.requests)
                }
            }
        }

    suspend fun pauseRequestResponse(stationId: Int): PauseRequestResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.pauseRequestQueue(stationId).also {
                it.user?.let { user -> updateUser(stationId, user) }
            }
        }

    suspend fun resumeRequestResponse(stationId: Int): ResumeRequestResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.resumeRequestQueue(stationId).also {
                it.user?.let { user -> updateUser(stationId, user) }
            }
        }

    suspend fun clearRequests(stationId: Int): ClearRequestsResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.clearRequests(stationId).also {
                updateRequests(stationId, it.requests)
            }
        }

    suspend fun requestFave(stationId: Int): RequestFaveResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.requestFave(stationId).also {
                if (it.result.success) {
                    updateRequests(stationId, it.requests)
                }
            }
        }

    suspend fun requestUnrated(stationId: Int): RequestUnratedResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.requestUnrated(stationId).also {
                if (it.result.success) {
                    updateRequests(stationId, it.requests)
                }
            }
        }

    private fun populateStationName(requests: List<Request>) {
        if (requests.isEmpty()) {
            return
        }
        stationsCache?.let {
            val stationMap = it.associate { station -> station.id to station.name }
            for (r in requests) {
                r.stationName = stationMap[r.stationId] ?: ""
            }
        }
    }

    private fun updateUser(stationId: Int, user: User) {
        getCurrentStationInfo(stationId)?.user = user
    }

    private fun updateRequests(stationId: Int, requests: List<Request>) {
        populateStationName(requests)
        getCurrentStationInfo(stationId)?.requests = requests
    }

    suspend fun removeSongRating(stationId: Int, songId: Int): RateSongResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.clearRating(stationId, songId).also { response ->
                updateInfoResponse(stationId, songId, response)
            }
        }

    suspend fun rateSong(stationId: Int, songId: Int, rating: Float): RateSongResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.rateSong(stationId, songId, rating).also { response ->
                updateInfoResponse(stationId, songId, response)
            }
        }

    private fun updateInfoResponse(stationId: Int, songId: Int, response: RateSongResponse) {
        if (!response.result.success) return
        val infoResponse = getCurrentStationInfo(stationId) ?: return
        val currentSong = infoResponse.getCurrentSong()
        if (currentSong.id == songId) {
            currentSong.ratingUser = response.result.rating
            return
        }
        (infoResponse.previousEvents.asSequence() + infoResponse.futureEvents.asSequence())
            .flatMap { it.songs }
            .firstOrNull { it.id == songId }
            ?.let { it.ratingUser = response.result.rating }
    }

    suspend fun search(stationId: Int, query: String): SearchResponse =
        withContext(coroutineDispatchers.network) {
            rainwaveService.search(stationId, query)
        }

    suspend fun autoRequestSongs(stationId: Int, infoResponse: InfoResponse) = withContext(coroutineDispatchers.compute) {
        Timber.d("Running auto request for station id %d", stationId)

        if (infoResponse.user.requestsPaused) {
            Timber.d("Requests are paused for station id %d, so skipping auto request", stationId)
            return@withContext
        }

        val autoRequestFaveEnabled = dataStore.get(AUTO_REQUEST_FAVE)
        val autoRequestUnratedEnabled = dataStore.get(AUTO_REQUEST_UNRATED)
        val autoClearRequestsEnabled = dataStore.get(AUTO_REQUEST_CLEAR)

        val initialRequests = infoResponse.requests.toList()
        val numOfRequests = initialRequests.size

        var requests = initialRequests

        if (autoClearRequestsEnabled && numOfRequests > 0) {
            val requestsToDelete = initialRequests.filter { it.cool || !it.good }
            if (requestsToDelete.isNotEmpty()) {
                if (requestsToDelete.size == numOfRequests) {
                    Timber.d("Clearing requests")
                    requests = clearRequests(stationId).requests
                } else {
                    Timber.d("Deleting %d requests", requestsToDelete.size)
                    requestsToDelete.forEach { request ->
                        requests = deleteRequest(request.stationId, request.songId).requests
                    }
                }
            }
        }

        if (requests.isEmpty() && autoRequestFaveEnabled) {
            Timber.d("Requesting fave")
            requests = requestFave(stationId).requests
        }

        if (requests.isEmpty() && autoRequestUnratedEnabled) {
            Timber.d("Requesting unrated")
            requestUnrated(stationId)
        }

        Timber.d("Completed auto request.")
    }

    fun allFaves(pagingConfig: PagingConfig): Pager<Int, Song> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { AllFavesPagingSource(rainwaveService) }
        )
    }

    fun recentVotes(stationId: Int, pagingConfig: PagingConfig): Pager<Int, Song> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { RecentVotesPagingSource(rainwaveService, stationId) }
        )
    }

    fun requestHistory(stationId: Int, pagingConfig: PagingConfig): Pager<Int, Song> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { RequestHistoryPagingSource(rainwaveService, stationId) }
        )
    }
}

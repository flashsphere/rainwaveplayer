package com.flashsphere.rainwaveplayer.flow

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.ApiResponse
import com.flashsphere.rainwaveplayer.model.FailureApiResponse
import com.flashsphere.rainwaveplayer.model.SuccessApiResponse
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.getTimeRemaining
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StationInfoProvider(
    private val rainwaveService: RainwaveService,
    private val userRepository: UserRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val stationId: Int,
) {
    private val stationInfoSharedFlow = MutableSharedFlow<InfoResponse>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val stationInfoFlow = createFetchStationInfoFlow()
    private val stationInfoRepeatedFlow = createRepeatedFetchFlow(stationInfoFlow)

    val flow = create()

    @Volatile
    private var refreshAtNextSubscription = false
    private var delayedRefreshJob: Job? = null

    fun getCachedInfoResponse(): InfoResponse? = stationInfoSharedFlow.replayCache.firstOrNull()

    fun hasObservers(): Boolean = (stationInfoSharedFlow.subscriptionCount.value > 0)

    fun refreshAtNextSubscription(delayInSeconds: Int = 0, fn: suspend () -> Unit = {}): Job? {
        cancel(delayedRefreshJob)

        val action = {
            Timber.d("Setting refreshAtNextSubscription to true")
            refreshAtNextSubscription = true
            coroutineDispatchers.scope.launch { fn() }
        }

        if (delayInSeconds == 0) {
            action()
            return null
        }

        delayedRefreshJob = coroutineDispatchers.scope.launchWithDefaults("Delayed Station Info Refresh") {
            try {
                delay(delayInSeconds.seconds)
                action()
            } finally {
                delayedRefreshJob = null
            }
        }
        return delayedRefreshJob
    }

    private fun create(): Flow<InfoResponse> {
        Timber.d("create")

        val flow1 = stationInfoSharedFlow
            .filter { it.getTimeRemaining() > 0 }

        val flow2 = stationInfoRepeatedFlow
            .onStart {
                val timeRemaining = getTimeRemainingForCache()
                if (timeRemaining > 0) {
                    Timber.d("next song delay start for station %d = %ds", stationId, timeRemaining)
                    delay(timeRemaining.seconds)
                }
            }
            .onStart {
                if (refreshAtNextSubscription) {
                    refreshAtNextSubscription = false
                    if (getTimeRemainingForCache() > 0) {
                        Timber.d("Refresh triggered")
                        stationInfoFlow.collect()
                    }
                }
            }
            .map { apiResponse ->
                when (apiResponse) {
                    is SuccessApiResponse -> apiResponse.data
                    is FailureApiResponse -> throw apiResponse.exception
                }
            }
            .filter { false }

        return merge(flow1, flow2)
            .flowOn(coroutineDispatchers.compute)
    }

    private fun createFetchStationInfoFlow(): Flow<InfoResponse> {
        return flow { emit(rainwaveService.fetchStationInfo(stationId)) }
            .flowOn(coroutineDispatchers.network)
            .onEach { infoResponse ->
                val userCredentials = userRepository.getCredentials() ?: return@onEach

                var found = false
                val userId = userCredentials.userId

                for (e in infoResponse.futureEvents) {
                    var userRequestedSong: Song? = null
                    for (s in e.songs) {
                        if (s.userIdRequested == userId) {
                            userRequestedSong = s
                        }

                        found = infoResponse.isSongVoted(e.id, s.entryId)
                        s.voted = found

                        if (found) {
                            break
                        }
                    }
                    if (found) {
                        break
                    } else if (userRequestedSong != null) {
                        userRequestedSong.voted = true
                    }
                }
            }
            .onEach { stationInfoSharedFlow.emit(it) }
            .flowOn(coroutineDispatchers.compute)
    }

    private fun createRepeatedFetchFlow(fetchFlow: Flow<InfoResponse>): Flow<ApiResponse<InfoResponse>> {
        return fetchFlow
            .repeatWhen { infoResponse ->
                if (infoResponse == null) {
                    delay(1.seconds)
                } else {
                    Timber.d("system time = %s", System.currentTimeMillis().milliseconds.inWholeSeconds)
                    Timber.d("api time = %s", infoResponse.apiInfo.time)
                    Timber.d("difference = %s", infoResponse.apiInfo.timeDifference)
                    Timber.d("event end time = %s", infoResponse.currentEvent.endTime)

                    val delaySeconds = infoResponse.getTimeRemaining().coerceAtLeast(1)
                    Timber.d("next song delay for station %d = %ds", stationId, delaySeconds)
                    delay(delaySeconds.seconds)
                }
                true
            }
            .map { ApiResponse.ofSuccess(it) }
            .catch { emit(ApiResponse.ofFailure(it)) }
            .shareIn(coroutineDispatchers.scope, WhileSubscribed(replayExpirationMillis = 0))
    }

    private fun getTimeRemainingForCache(): Long {
        return getCachedInfoResponse()?.getTimeRemaining() ?: 0L
    }
}

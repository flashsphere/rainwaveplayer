package com.flashsphere.rainwaveplayer.flow

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.InfoResponseState
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.getTimeRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StationInfoProvider(
    private val rainwaveService: RainwaveService,
    private val userRepository: UserRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val stationId: Int,
) {
    @Volatile
    private var cachedData: InfoResponse? = null
    @Volatile
    private lateinit var subscriptionCountFlow: StateFlow<Int>
    private val refreshPending = MutableStateFlow(false)
    private val retryOnSubscription = MutableStateFlow(false)
    private var delayedRefreshJob: Job? = null

    val flow: SharedFlow<InfoResponseState> =
        flow {
            var duration = cachedData
                ?.getTimeRemaining()
                ?.coerceAtLeast(0)
                ?.seconds
                ?: Duration.ZERO

            while (currentCoroutineContext().isActive) {
                awaitRefresh(duration)
                duration = fetch(this)
            }
        }
        .buffer(0)
        .shareIn(
            scope = coroutineDispatchers.scope + CoroutineName("Station info shared flow"),
            started = { count ->
                subscriptionCountFlow = count
                SharingStarted.WhileSubscribed().command(count)
            },
            replay = 0,
        )
        .onSubscription {
            if (retryOnSubscription.compareAndSet(expect = true, update = false)) {
                refresh()
            }
            cachedData?.let { emit(InfoResponseState(data = it)) }
        }

    private suspend fun fetch(
        collector: FlowCollector<InfoResponseState>,
    ): Duration {
        val loadingState = InfoResponseState(
            data = cachedData,
            loading = true
        )
        collector.emit(loadingState)

        val consumedRefreshRequest = refreshPending.compareAndSet(expect = true, update = false)

        val infoResponse = try {
            suspendRunCatching {
                withContext(coroutineDispatchers.io) {
                    rainwaveService.fetchStationInfo(stationId).also {
                        postProcess(it)
                    }
                }
            }.getOrElse { throwable ->
                retryOnSubscription.value = true

                val errorState = InfoResponseState(data = cachedData, error = throwable)
                collector.emit(errorState)
                return Duration.INFINITE
            }
        } catch (e: CancellationException) {
            if (consumedRefreshRequest) {
                refreshPending.value = true
            }
            throw e
        }
        retryOnSubscription.value = false

        cachedData = infoResponse
        val loadedState = InfoResponseState(
            data = infoResponse,
            loading = infoResponse.getTimeRemaining() <= 0,
        )
        collector.emit(loadedState)

        Timber.d("system time = %s", System.currentTimeMillis().milliseconds.inWholeSeconds)
        Timber.d("api time = %s", infoResponse.apiInfo.time)
        Timber.d("difference = %s", infoResponse.apiInfo.timeDifference)
        Timber.d("event end time = %s", infoResponse.currentEvent.endTime)
        val delaySeconds = infoResponse.getTimeRemaining().coerceAtLeast(1)
        Timber.d("next song delay for station %d = %ds", stationId, delaySeconds)

        return delaySeconds.seconds
    }

    private suspend fun awaitRefresh(duration: Duration) {
        Timber.d("Awaiting refresh: %s", duration)
        val refreshSignal = withTimeoutOrNull(duration) {
            combine(refreshPending, subscriptionCountFlow) { pending, count ->
                pending && count > 0
            }.first { it }
        }
        Timber.d(
            if (refreshSignal != null) "Refresh signal received"
            else "Refresh signal timeout"
        )
    }

    private fun postProcess(infoResponse: InfoResponse) {
        val userCredentials = userRepository.getCredentials() ?: return

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

    fun getCachedInfoResponse(): InfoResponse? {
        return cachedData
    }

    fun hasObservers(): Boolean {
        return ::subscriptionCountFlow.isInitialized && subscriptionCountFlow.value > 0
    }

    fun refreshWithDelay(delayInSeconds: Int = 0): Job? {
        cancel(delayedRefreshJob)

        if (delayInSeconds <= 0) {
            refresh()
            return null
        }

        delayedRefreshJob = coroutineDispatchers.scope.launchWithDefaults("Delayed Station Info Refresh") {
            delay(delayInSeconds.seconds)
            refresh()
        }
        return delayedRefreshJob
    }

    fun refresh() {
        Timber.d("Sending refresh signal")
        refreshPending.value = true
    }
}

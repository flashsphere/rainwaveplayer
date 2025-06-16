package com.flashsphere.rainwaveplayer.view.viewmodel

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.song.RateSongResponse
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.ResponseBody
import retrofit2.Converter

class RateSongDelegate(
    private val stationRepository: StationRepository,
    private val rateSongResponseConverter: Converter<ResponseBody, RateSongResponse>,
) {
    private val _rateSongState = MutableSharedFlow<RateSongState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = SUSPEND
    )
    val rateSongState = _rateSongState.asSharedFlow()

    fun rateSong(scope: CoroutineScope, stationId: Int, song: SongState, rating: Float): Job {
        return if (rating > 0F) {
            scope.launchWithDefaults("Rate Song") {
                suspendRunCatching { stationRepository.rateSong(stationId, song.id, rating) }
                    .onFailure { e ->
                        emitError(stationId, song, rating,
                            e.toOperationError(rateSongResponseConverter))
                    }
                    .onSuccess {
                        if (it.result.success) {
                            song.ratingUser.floatValue = it.result.rating
                            emitSuccess(stationId, song)
                        } else {
                            emitError(stationId, song, rating, it)
                        }
                    }
            }
        } else {
            removeRating(scope, stationId, song)
        }
    }

    fun rateSong(scope: CoroutineScope, stationId: Int, songId: Int, rating: Float,
                 onSuccess: (message: String) -> Unit,
                 onFailure: (error: OperationError) -> Unit
    ): Job {
        return if (rating > 0F) {
            scope.launchWithDefaults("Rate Song") {
                suspendRunCatching { stationRepository.rateSong(stationId, songId, rating) }
                    .onFailure { e ->
                        onFailure(e.toOperationError(rateSongResponseConverter))
                    }
                    .onSuccess {
                        if (it.result.success) {
                            onSuccess(it.result.text)
                        } else {
                            onFailure(OperationError(OperationError.Server, it.result.text))
                        }
                    }
            }
        } else {
            removeRating(scope, stationId, songId, onSuccess, onFailure)
        }
    }

    private suspend fun emitSuccess(stationId: Int, song: SongState) {
        _rateSongState.emit(
            RateSongState(
                success = true,
                stationId = stationId,
                song = song,
                rating = song.ratingUser.floatValue,
            )
        )
    }

    private suspend fun emitError(stationId: Int, song: SongState, rating: Float,
                                  error: OperationError) {
        _rateSongState.emit(
            RateSongState(
                success = false,
                stationId = stationId,
                song = song,
                rating = rating,
                error = error,
            )
        )
    }

    private suspend fun emitError(stationId: Int, song: SongState, rating: Float,
                                  response: RateSongResponse) {
        emitError(stationId, song, rating,
            OperationError(OperationError.Server, response.result.text))
    }

    private fun removeRating(scope: CoroutineScope, stationId: Int, song: SongState): Job {
        return scope.launchWithDefaults("Remove Song Rating") {
            suspendRunCatching { stationRepository.removeSongRating(stationId, song.id) }
                .onFailure { e ->
                    emitError(stationId, song, 0F,
                        e.toOperationError(rateSongResponseConverter))
                }
                .onSuccess {
                    if (it.result.success) {
                        song.ratingUser.floatValue = it.result.rating
                        emitSuccess(stationId, song)
                    } else {
                        emitError(stationId, song, 0F, it)
                    }
                }
        }
    }

    private fun removeRating(scope: CoroutineScope, stationId: Int, songId: Int,
                             onSuccess: (message: String) -> Unit,
                             onFailure: (error: OperationError) -> Unit
    ): Job {
        return scope.launchWithDefaults("Rate Song") {
            suspendRunCatching { stationRepository.removeSongRating(stationId, songId) }
                .onFailure { e ->
                    onFailure(e.toOperationError(rateSongResponseConverter))
                }
                .onSuccess {
                    if (it.result.success) {
                        onSuccess(it.result.text)
                    } else {
                        onFailure(OperationError(OperationError.Server, it.result.text))
                    }
                }
        }
    }
}

data class RateSongState(
    val success: Boolean,
    val stationId: Int,
    val song: SongState,
    val rating: Float,
    val error: OperationError? = null,
)


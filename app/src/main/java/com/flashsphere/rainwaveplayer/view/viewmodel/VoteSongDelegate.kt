package com.flashsphere.rainwaveplayer.view.viewmodel

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.model.vote.VoteResponse
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.ResponseBody
import retrofit2.Converter

class VoteSongDelegate(
    private val stationRepository: StationRepository,
    private val voteResponseConverter: Converter<ResponseBody, VoteResponse>,
) {
    private val _voteSongState = MutableSharedFlow<VoteSongState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = SUSPEND
    )
    val voteSongState = _voteSongState.asSharedFlow()

    suspend fun voteSong(stationId: Int, eventId: Int, entryId: Int) {
        suspendRunCatching { stationRepository.vote(stationId, entryId) }
            .onFailure { e ->
                val error = e.toOperationError(voteResponseConverter).let {
                    if (it.responseResult?.tlKey == "tunein_required") {
                        OperationError(Server, it.message, it.responseResult)
                    } else {
                        it
                    }
                }

                _voteSongState.emit(VoteSongState(
                    success = false,
                    stationId = stationId,
                    eventId = eventId,
                    entryId = entryId,
                    error = error
                ))
            }
            .onSuccess {
                if (it.result.success) {
                    _voteSongState.emit(VoteSongState(
                        success = true,
                        stationId = stationId,
                        eventId = it.result.eventId,
                        entryId = it.result.entryId,
                    ))
                } else {
                    _voteSongState.emit(VoteSongState(
                        success = false,
                        stationId = stationId,
                        eventId = eventId,
                        entryId = entryId,
                        error = OperationError(Server, it.result.text)
                    ))
                }
                it.result.success
            }
    }

    fun voteSong(scope: CoroutineScope, stationId: Int, eventId: Int, entryId: Int): Job {
        return scope.launchWithDefaults("Vote Song") {
            voteSong(stationId, eventId, entryId)
        }
    }
}

data class VoteSongState(
    val success: Boolean,
    val stationId: Int,
    val eventId: Int,
    val entryId: Int,
    val error: OperationError? = null,
)

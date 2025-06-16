package com.flashsphere.rainwaveplayer.view.viewmodel

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.request.RequestSongResponse
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import okhttp3.ResponseBody
import retrofit2.Converter

class RequestSongDelegate(
    private val scope: CoroutineScope,
    private val stationRepository: StationRepository,
    private val requestSongResponseConverter: Converter<ResponseBody, RequestSongResponse>,
) {
    fun requestSong(station: Station, song: SongState,
                    onSuccess: () -> Unit,
                    onFail: (error: OperationError) -> Unit): Job {
        return scope.launchWithDefaults("Request Song") {
            suspendRunCatching { stationRepository.requestSong(station.id, song.id) }
                .onFailure { e ->
                    onFail(e.toOperationError(requestSongResponseConverter))
                }
                .onSuccess {
                    if (it.result.success) {
                        onSuccess()
                    } else {
                        onFail(OperationError(OperationError.Server, it.result.text))
                    }
                }
        }
    }
}

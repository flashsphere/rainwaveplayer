package com.flashsphere.rainwaveplayer.view.viewmodel

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.favorite.FaveAlbumResponse
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.ResponseBody
import retrofit2.Converter

class FaveAlbumDelegate(
    private val stationRepository: StationRepository,
    private val faveAlbumResponseConverter: Converter<ResponseBody, FaveAlbumResponse>
) {
    private val _faveAlbumState = MutableSharedFlow<FaveAlbumState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = SUSPEND
    )
    val faveAlbumState = _faveAlbumState.asSharedFlow()

    fun faveAlbum(scope: CoroutineScope, station: Station, album: AlbumState) {
        scope.launchWithDefaults("Fave Album") {
            suspendRunCatching { stationRepository.favoriteAlbum(station.id, album.id, !album.favorite.value) }
                .onFailure { e ->
                    _faveAlbumState.emit(
                        FaveAlbumState(
                            success = false,
                            station = station,
                            album = album,
                            error = e.toOperationError(faveAlbumResponseConverter),
                        )
                    )
                }
                .onSuccess {
                    if (it.result.success) {
                        album.favorite.value = it.result.favorite
                        _faveAlbumState.emit(
                            FaveAlbumState(
                                success = true,
                                station = station,
                                album = album,
                            )
                        )
                    } else {
                        _faveAlbumState.emit(
                            FaveAlbumState(
                                success = false,
                                station = station,
                                album = album,
                                error = OperationError(OperationError.Server, it.result.text),
                            )
                        )
                    }
                }
        }
    }
}

data class FaveAlbumState(
    val success: Boolean,
    val station: Station,
    val album: AlbumState,
    val error: OperationError? = null,
)

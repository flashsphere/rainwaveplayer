package com.flashsphere.rainwaveplayer.view.viewmodel

import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.favorite.FaveSongResponse
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

class FaveSongDelegate(
    private val stationRepository: StationRepository,
    private val faveSongResponseConverter: Converter<ResponseBody, FaveSongResponse>,
) {
    private val _faveSongState = MutableSharedFlow<FaveSongState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = SUSPEND
    )
    val faveSongState = _faveSongState.asSharedFlow()

    fun faveSong(scope: CoroutineScope, songId: Int, favorite: Boolean): Job {
        return scope.launchWithDefaults("Fave Song") {
            suspendRunCatching { stationRepository.favoriteSong(songId, favorite) }
                .onFailure { e ->
                    _faveSongState.emit(FaveSongState(
                        success = false,
                        songId = songId,
                        favorite = !favorite,
                        song = null,
                        error = e.toOperationError(faveSongResponseConverter)))
                }
                .onSuccess {
                    if (it.result.success) {
                        _faveSongState.emit(FaveSongState(
                            success = true,
                            songId = songId,
                            favorite = it.result.favorite,
                            song = null))
                    } else {
                        _faveSongState.emit(FaveSongState(
                            success = false,
                            songId = songId,
                            favorite = !favorite,
                            song = null,
                            error = OperationError(OperationError.Server, it.result.text)
                        ))
                    }
                }
        }
    }

    fun faveSong(scope: CoroutineScope, song: SongState): Job {
        return scope.launchWithDefaults("Fave Song") {
            suspendRunCatching { stationRepository.favoriteSong(song.id, !song.favorite.value) }
                .onFailure { e ->
                    _faveSongState.emit(FaveSongState(
                        success = false,
                        songId = song.id,
                        favorite = song.favorite.value,
                        song = song,
                        error = e.toOperationError(faveSongResponseConverter)))
                }
                .onSuccess {
                    if (it.result.success) {
                        song.favorite.value = it.result.favorite
                        _faveSongState.emit(FaveSongState(
                            success = true,
                            songId = song.id,
                            favorite = it.result.favorite,
                            song = song))
                    } else {
                        _faveSongState.emit(FaveSongState(
                            success = false,
                            songId = song.id,
                            favorite = song.favorite.value,
                            song = song,
                            error = OperationError(OperationError.Server, it.result.text)
                        ))
                    }
                }
        }
    }
}

data class FaveSongState(
    val success: Boolean,
    val songId: Int,
    val favorite: Boolean,
    val song: SongState?,
    val error: OperationError? = null,
)

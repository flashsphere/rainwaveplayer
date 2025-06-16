package com.flashsphere.rainwaveplayer.view.uistate

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState

data class AlbumScreenState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val album: AlbumState? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun init(album: AlbumState): AlbumScreenState {
            return AlbumScreenState(loading = false, loaded = false, album = album, error = null)
        }

        fun loading(state: AlbumScreenState): AlbumScreenState {
            return state.copy(loading = true, error = null)
        }

        fun loaded(album: AlbumState): AlbumScreenState {
            return AlbumScreenState(loading = false, loaded = true, album = album, error = null)
        }

        fun error(state: AlbumScreenState, error: OperationError): AlbumScreenState {
            return state.copy(loading = false, error = error)
        }
    }
}

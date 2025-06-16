package com.flashsphere.rainwaveplayer.view.uistate.tv

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem

data class SongActionsScreenState(
    val loading: Boolean = false,
    val songItem: StationInfoSongItem? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun loading(): SongActionsScreenState {
            return SongActionsScreenState(loading = true)
        }
        fun loaded(songItem: StationInfoSongItem): SongActionsScreenState {
            return SongActionsScreenState(
                songItem = songItem,
            )
        }
        fun error(error: OperationError): SongActionsScreenState {
            return SongActionsScreenState(error = error)
        }
    }
}

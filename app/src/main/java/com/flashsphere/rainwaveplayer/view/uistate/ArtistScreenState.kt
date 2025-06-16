package com.flashsphere.rainwaveplayer.view.uistate

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState

data class ArtistScreenState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val artist: ArtistState? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun init(artist: ArtistState): ArtistScreenState {
            return ArtistScreenState(loading = false, loaded = false, artist = artist,
                error = null)
        }

        fun loading(state: ArtistScreenState): ArtistScreenState {
            return state.copy(loading = true, error = null)
        }

        fun loaded(artist: ArtistState): ArtistScreenState {
            return ArtistScreenState(loading = false, loaded = true, artist = artist,
                error = null)
        }

        fun error(state: ArtistScreenState, error: OperationError): ArtistScreenState {
            return state.copy(loading = false, error = error)
        }
    }
}

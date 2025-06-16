package com.flashsphere.rainwaveplayer.view.uistate

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfo

data class StationInfoScreenState(
    val loading: Boolean = false,
    val stationInfo: StationInfo? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun loading(state: StationInfoScreenState): StationInfoScreenState {
            return state.copy(loading = true, error = null)
        }

        fun loaded(stationInfo: StationInfo): StationInfoScreenState {
            return StationInfoScreenState(loading = false, stationInfo = stationInfo, error = null)
        }

        fun error(error: OperationError): StationInfoScreenState {
            return StationInfoScreenState(loading = false, stationInfo = null, error = error)
        }

        fun clear(): StationInfoScreenState {
            return StationInfoScreenState()
        }
    }
}

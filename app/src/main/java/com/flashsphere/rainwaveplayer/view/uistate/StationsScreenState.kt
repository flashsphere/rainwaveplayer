package com.flashsphere.rainwaveplayer.view.uistate

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.util.OperationError

data class StationsScreenState(
    val loading: Boolean = false,
    val stations: SnapshotStateList<Station>? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun loading(): StationsScreenState {
            return StationsScreenState(
                loading = true,
                stations = null,
                error = null
            )
        }

        fun loaded(stations: List<Station>): StationsScreenState {
            return StationsScreenState(
                loading = false,
                stations = stations.toMutableStateList(),
                error = null
            )
        }

        fun error(operationError: OperationError): StationsScreenState {
            return StationsScreenState(
                loading = false,
                stations = null,
                error = operationError
            )
        }
    }
}

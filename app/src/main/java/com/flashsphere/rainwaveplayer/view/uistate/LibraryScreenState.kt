package com.flashsphere.rainwaveplayer.view.uistate

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.LibraryItem

data class LibraryScreenState<T : LibraryItem>(
    val loading: Boolean = false,
    val station: Station? = null,
    val data: SnapshotStateList<T>? = null,
    val filteredData: SnapshotStateList<T>? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun <T : LibraryItem> loading(state: LibraryScreenState<T>,
                                      station: Station): LibraryScreenState<T> {
            return if (state.station != station) {
                LibraryScreenState(loading = true, station = station)
            } else {
                state.copy(loading = true, station = station, error = null)
            }
        }

        fun <T : LibraryItem> loaded(station: Station,
                                     data: SnapshotStateList<T>,
                                     filteredData: SnapshotStateList<T>): LibraryScreenState<T> {
            return LibraryScreenState(loading = false, station = station,
                data = data, filteredData = filteredData, error = null)
        }

        fun <T : LibraryItem> error(state: LibraryScreenState<T>, error: OperationError): LibraryScreenState<T> {
            return state.copy(loading = false, error = error)
        }
    }
}

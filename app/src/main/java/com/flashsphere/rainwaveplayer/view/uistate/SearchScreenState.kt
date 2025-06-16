package com.flashsphere.rainwaveplayer.view.uistate

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchResult

data class SearchScreenState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val result: SearchResult? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun loading(): SearchScreenState {
            return SearchScreenState(loading = true, loaded = false, result = null, error = null)
        }

        fun loaded(result: SearchResult): SearchScreenState {
            return SearchScreenState(loading = false, loaded = true, result = result, error = null)
        }

        fun error(error: OperationError): SearchScreenState {
            return SearchScreenState(loading = false, loaded = false, result = null, error = error)
        }
    }
}

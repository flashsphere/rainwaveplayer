package com.flashsphere.rainwaveplayer.view.uistate

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flashsphere.rainwaveplayer.model.request.Request
import com.flashsphere.rainwaveplayer.model.user.User
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState

data class RequestsScreenState(
    val loading: Boolean = false,
    val requests: SnapshotStateList<RequestState>? = null,
    val originalRequests: List<RequestState>? = null,
    val user: UserState? = null,
    val error: OperationError? = null,
) {
    companion object {
        fun loading(state: RequestsScreenState): RequestsScreenState {
            return state.copy(loading = true)
        }

        fun loaded(state: RequestsScreenState): RequestsScreenState {
            return state.copy(loading = false)
        }

        fun loaded(requests: List<Request>, user: User): RequestsScreenState {
            val processedRequests = processRequests(requests)
            return RequestsScreenState(
                loading = false,
                user = UserState(user),
                requests = processedRequests,
                originalRequests = processedRequests.toList(),
                error = null
            )
        }

        fun requestsUpdated(state: RequestsScreenState, requests: List<Request>): RequestsScreenState {
            val processedRequests = processRequests(requests)
            return state.copy(
                loading = false,
                requests = processedRequests,
                originalRequests = processedRequests.toList(),
                error = null
            )
        }

        fun userUpdated(state: RequestsScreenState, user: UserState): RequestsScreenState {
            return state.copy(loading = false, user = user, error = null)
        }

        fun error(state: RequestsScreenState, error: OperationError): RequestsScreenState {
            return state.copy(loading = false, error = error)
        }

        private fun processRequests(requests: List<Request>): SnapshotStateList<RequestState> {
            return requests.asSequence()
                .distinctBy { it.songId }
                .map { RequestState(it) }
                .toCollection(mutableStateListOf())
        }
    }
}

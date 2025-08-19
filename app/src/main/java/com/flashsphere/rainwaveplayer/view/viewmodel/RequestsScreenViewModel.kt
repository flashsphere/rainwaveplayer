package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.flow.autoRetry
import com.flashsphere.rainwaveplayer.model.request.ClearRequestsErrorResponse
import com.flashsphere.rainwaveplayer.model.request.DeleteRequestResponse
import com.flashsphere.rainwaveplayer.model.request.OrderRequestsResponse
import com.flashsphere.rainwaveplayer.model.request.PauseRequestResponse
import com.flashsphere.rainwaveplayer.model.request.RequestFaveResponse
import com.flashsphere.rainwaveplayer.model.request.RequestUnratedResponse
import com.flashsphere.rainwaveplayer.model.request.ResumeRequestResponse
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.RequestsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.RequestErrorEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import okhttp3.ResponseBody
import retrofit2.Converter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class RequestsScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val uiEventDelegate: UiEventDelegate,
    @Named("order_requests_converter")
    private val orderRequestsResponseConverter: Converter<ResponseBody, OrderRequestsResponse>,
    @Named("delete_request_converter")
    private val deleteRequestConverter: Converter<ResponseBody, DeleteRequestResponse>,
    @Named("pause_request_converter")
    private val pauseRequestConverter: Converter<ResponseBody, PauseRequestResponse>,
    @Named("resume_request_converter")
    private val resumeRequestConverter: Converter<ResponseBody, ResumeRequestResponse>,
    @Named("clear_requests_converter")
    private val clearRequestsErrorResponseConverter: Converter<ResponseBody, ClearRequestsErrorResponse>,
    @Named("request_fave_converter")
    private val requestFaveResponseConverter: Converter<ResponseBody, RequestFaveResponse>,
    @Named("request_unrated_converter")
    private val requestUnratedResponseConverter: Converter<ResponseBody, RequestUnratedResponse>,
) : ViewModel() {
    val snackbarEvents = uiEventDelegate.snackbarEvents

    private var station: Station? = null

    private val _requestsScreenState = MutableStateFlow(RequestsScreenState())
    val requestsScreenState = _requestsScreenState.asStateFlow()

    private var requestsJob: Job? = null
    private var suspendResumeJob: Job? = null
    private var clearRequestsJob: Job? = null
    private var requestFaveJob: Job? = null
    private var requestUnratedJob: Job? = null

    fun subscribeStationInfo(station: Station, refresh: Boolean = false) {
        this.station = station
        subscribeStationInfo(refresh)
    }

    fun subscribeStationInfo(refresh: Boolean = false) {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        cancel(requestsJob)
        requestsJob = stationRepository.getStationInfoFlow(station.id, refresh)
            .map { infoResponse ->
                if (refresh) {
                    // workaround Pull To Refresh indicator not disappearing when force refresh
                    // because the API returns the response too quickly
                    delay(100)
                }
                RequestsScreenState.loaded(infoResponse.requests, infoResponse.user)
            }
            .flowOn(coroutineDispatchers.compute)
            .autoRetry(connectivityObserver, coroutineDispatchers) {
                _requestsScreenState.value = RequestsScreenState.error(_requestsScreenState.value,
                    OperationError(OperationError.Server))
            }
            .onEach { _requestsScreenState.value = it }
            .launchWithDefaults(viewModelScope, "Requests List")
    }

    fun unsubscribeStationInfo() {
        cancel(requestsJob)
    }

    fun reorderRequestItem(fromIndex: Int, toIndex: Int) {
        val requests = _requestsScreenState.value.requests ?: return
        val fromItem = requests[fromIndex]
        requests[fromIndex] = requests[toIndex]
        requests[toIndex] = fromItem
    }

    fun reorderItemToTop(request: RequestState, index: Int) {
        val requests = _requestsScreenState.value.requests ?: return
        val item = requests.getOrNull(index) ?: return
        if (item.songId != request.songId) return

        requests.add(0, requests.removeAt(index))
        reorderRequests(requests)
    }

    fun reorderRequests() {
        val requestsScreenState = _requestsScreenState.value
        val originalRequests = requestsScreenState.originalRequests?.joinToString(",") { it.songId.toString() } ?: return
        val requests = requestsScreenState.requests?.joinToString(",") { it.songId.toString() } ?: return
        if (requests == originalRequests) return

        reorderRequests(requestsScreenState.requests)
    }

    private fun reorderRequests(requests: List<RequestState>) {
        val station = this.station ?: return
        val songIds = requests.joinToString(",") { it.songId.toString() }

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        viewModelScope.launchWithDefaults("Reorder Requests") {
            suspendRunCatching { stationRepository.orderRequests(station.id, songIds) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(orderRequestsResponseConverter),
                        retry = { reorderRequests() },
                        message = R.string.error_order_requests_failed))
                }
                .onSuccess { response ->
                    if (response.result.success) {
                        _requestsScreenState.value = RequestsScreenState.requestsUpdated(
                            _requestsScreenState.value, response.requests)
                    } else {
                        _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                        uiEventDelegate.send(RequestErrorEvent(
                            error = OperationError(OperationError.Server, response.result.text),
                            retry = { reorderRequests() },
                            message = R.string.error_order_requests_failed))
                    }
                }
        }
    }

    fun deleteRequest(request: RequestState, index: Int): Boolean {
        _requestsScreenState.value.requests?.let { requests ->
            val item = requests.getOrNull(index) ?: return false
            if (item.songId != request.songId) return false

            requests.removeAt(index)
            deleteRequest(request)
            return true
        }
        return false
    }

    private fun deleteRequest(request: RequestState) {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        viewModelScope.launchWithDefaults("Delete Request") {
            suspendRunCatching { stationRepository.deleteRequest(station.id, request.songId) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(deleteRequestConverter),
                        retry = { deleteRequest(request) },
                        message = R.string.error_delete_request_failed))
                }
                .onSuccess {
                    if (it.result.success) {
                        _requestsScreenState.value = RequestsScreenState.requestsUpdated(
                            _requestsScreenState.value, it.requests)
                    } else {
                        _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                        uiEventDelegate.send(RequestErrorEvent(
                            error = OperationError(OperationError.Server, it.result.text),
                            retry = { deleteRequest(request) },
                            message = R.string.error_delete_request_failed))
                        subscribeStationInfo(true)
                    }
                }
        }
    }

    fun suspendQueue() {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        cancel(suspendResumeJob)
        suspendResumeJob = viewModelScope.launchWithDefaults("Suspend Queue") {
            suspendRunCatching { stationRepository.pauseRequestResponse(station.id) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(pauseRequestConverter),
                        retry = { suspendQueue() },
                        message = R.string.error_suspend_failed))
                }
                .onSuccess {
                    if (it.result.success) {
                        it.user?.let { user ->
                            _requestsScreenState.value = RequestsScreenState.userUpdated(
                                _requestsScreenState.value, UserState(user))
                        }
                    } else {
                        _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                        uiEventDelegate.send(RequestErrorEvent(
                            error = OperationError(OperationError.Server, it.result.text),
                            retry = { suspendQueue() },
                            message = R.string.error_suspend_failed))
                    }
                }
        }
    }

    fun resumeQueue() {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        cancel(suspendResumeJob)
        suspendResumeJob = viewModelScope.launchWithDefaults("Resume Queue") {
            suspendRunCatching { stationRepository.resumeRequestResponse(station.id) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(resumeRequestConverter),
                        retry = { resumeQueue() },
                        message = R.string.error_resume_failed))
                }
                .onSuccess {
                    if (it.result.success) {
                        it.user?.let { user ->
                            _requestsScreenState.value = RequestsScreenState.userUpdated(
                                _requestsScreenState.value, UserState(user))
                        }
                    } else {
                        _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                        uiEventDelegate.send(RequestErrorEvent(
                            error = OperationError(OperationError.Server, it.result.text),
                            retry = { resumeQueue() },
                            message = R.string.error_resume_failed))
                    }
                }
        }
    }

    fun clearRequests() {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        cancel(clearRequestsJob)
        clearRequestsJob = viewModelScope.launchWithDefaults("Clear Requests") {
            suspendRunCatching { stationRepository.clearRequests(station.id) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(clearRequestsErrorResponseConverter),
                        retry = { clearRequests() },
                        message = R.string.error_clear_failed))
                }
                .onSuccess {
                    _requestsScreenState.value = RequestsScreenState.requestsUpdated(
                        _requestsScreenState.value, it.requests)
                }
        }
    }

    fun requestFavorites() {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        cancel(requestFaveJob)
        requestFaveJob = viewModelScope.launchWithDefaults("Request Faves") {
            suspendRunCatching { stationRepository.requestFave(station.id) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(requestFaveResponseConverter),
                        retry = { requestFavorites() },
                        message = R.string.error_request_fave_failed))
                }
                .onSuccess {
                    if (it.result.success) {
                        _requestsScreenState.value = RequestsScreenState.requestsUpdated(
                            _requestsScreenState.value, it.requests)
                    } else {
                        _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                        uiEventDelegate.send(RequestErrorEvent(
                            error = OperationError(OperationError.Server, it.result.text),
                            retry = { requestFavorites() },
                            message = R.string.error_request_fave_failed))
                    }
                }
        }
    }

    fun requestUnrated() {
        val station = this.station ?: return

        uiEventDelegate.send(DismissSnackbarEvent)
        _requestsScreenState.value = RequestsScreenState.loading(_requestsScreenState.value)

        cancel(requestUnratedJob)
        requestUnratedJob = viewModelScope.launchWithDefaults("Request Unrated") {
            suspendRunCatching { stationRepository.requestUnrated(station.id) }
                .onFailure { e ->
                    _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                    uiEventDelegate.send(RequestErrorEvent(
                        error = e.toOperationError(requestUnratedResponseConverter),
                        retry = { requestUnrated() },
                        message = R.string.error_request_unrated_failed))
                }
                .onSuccess {
                    if (it.result.success) {
                        _requestsScreenState.value = RequestsScreenState.requestsUpdated(
                            _requestsScreenState.value, it.requests)
                    } else {
                        _requestsScreenState.value = RequestsScreenState.loaded(_requestsScreenState.value)
                        uiEventDelegate.send(RequestErrorEvent(
                            error = OperationError(OperationError.Server, it.result.text),
                            retry = { requestFavorites() },
                            message = R.string.error_request_unrated_failed))
                    }
                }
        }
    }

    fun showSnackbarMessage(message: String) {
        uiEventDelegate.sendSnackbarEvent(message)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        cancel(requestsJob)
    }
}

package com.flashsphere.rainwaveplayer.model.request

import kotlinx.serialization.Serializable

@Serializable
class ClearRequestsResponse(
    val requests: List<Request> = emptyList(),
)

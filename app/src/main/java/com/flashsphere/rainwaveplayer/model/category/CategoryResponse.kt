package com.flashsphere.rainwaveplayer.model.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CategoryResponse(
    @SerialName("group")
    val category: Category
)

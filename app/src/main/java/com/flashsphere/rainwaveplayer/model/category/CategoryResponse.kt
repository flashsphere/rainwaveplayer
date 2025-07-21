package com.flashsphere.rainwaveplayer.model.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CategoryResponse(
    @SerialName("group")
    @Serializable(with = CategorySerializer::class)
    val category: Category
)

package com.flashsphere.rainwaveplayer.model.category

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = AllCategoriesSerializer::class)
class AllCategories(
    val categories: List<Category> = emptyList()
)

@Serializable
private class AllCategoriesSurrogate(
    val all_groups: List<Category> = emptyList()
)

object AllCategoriesSerializer : KSerializer<AllCategories> {
    override val descriptor: SerialDescriptor = AllCategoriesSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): AllCategories {
        val surrogate = decoder.decodeSerializableValue(AllCategoriesSurrogate.serializer())
        return AllCategories(
            surrogate.all_groups.sortedWith(CATEGORY_NAME_COMPARATOR)
        )
    }

    override fun serialize(encoder: Encoder, value: AllCategories) {
    }
}

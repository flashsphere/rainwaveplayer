package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flashsphere.rainwaveplayer.model.category.Category

@Immutable
class CategoryState(
    override val id: Int,
    override val name: String,
    val items: SnapshotStateList<CategoryDetailItem>,
) : LibraryItem {
    val key: String = "category-${id}"
    override val searchable: String = name

    constructor(id: Int, name: String) : this(
        id = id,
        name = name,
        items = mutableStateListOf()
    )

    constructor(category: Category) : this(
        id = category.id,
        name = category.name,
        items = mutableStateListOf<CategoryDetailItem>().apply {
            for (entry in category.songs) {
                add(AlbumState(entry.key))
                entry.value.forEach { song ->
                    add(SongState(song))
                }
            }
        }
    )
}

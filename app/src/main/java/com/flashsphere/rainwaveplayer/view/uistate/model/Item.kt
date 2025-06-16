package com.flashsphere.rainwaveplayer.view.uistate.model

interface CategoryDetailItem {
    val key: String
}

interface ArtistDetailItem {
    val key: String
}

interface LibraryItem {
    val id: Int
    val name: String
    val searchable: String
}

interface SearchItem {
    val id: Int
    val key: String
}

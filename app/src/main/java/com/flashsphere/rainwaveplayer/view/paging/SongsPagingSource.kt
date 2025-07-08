package com.flashsphere.rainwaveplayer.view.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.flashsphere.rainwaveplayer.model.song.Song
import timber.log.Timber

abstract class SongsPagingSource() : PagingSource<Int, Song>() {
    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return null
    }

    abstract suspend fun fetchData(loadSize: Int, key: Int): List<Song>

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val key = params.key ?: 0
        Timber.d("key = %d, load size = %d", key, params.loadSize)

        return runCatching {
            val songs = fetchData(params.loadSize, key)
            LoadResult.Page(
                data = songs,
                prevKey = if (key == 0) null else key - params.loadSize,
                nextKey = if (songs.size < params.loadSize || songs.isEmpty()) null else key + params.loadSize
            )
        }.getOrElse {
            LoadResult.Error(it)
        }
    }
}

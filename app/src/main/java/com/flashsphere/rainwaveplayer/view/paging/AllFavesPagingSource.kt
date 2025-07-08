package com.flashsphere.rainwaveplayer.view.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import timber.log.Timber

class AllFavesPagingSource(
    private val rainwaveService: RainwaveService,
    private val stationId: Int,
) : PagingSource<Int, Song>() {
    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val key = params.key ?: 0
        Timber.d("key = %d, load size = %d", key, params.loadSize)

        return runCatching {
            val response = rainwaveService.allFaves(stationId, params.loadSize, key)
            val songs = response.songs
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

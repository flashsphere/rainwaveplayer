package com.flashsphere.rainwaveplayer.view.paging

import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.repository.RainwaveService

class RequestHistoryPagingSource(
    private val rainwaveService: RainwaveService,
    private val stationId: Int,
) : SongsPagingSource() {
    override suspend fun fetchData(loadSize: Int, key: Int): List<Song> {
        return rainwaveService.requestHistory(stationId, loadSize, key).songs
    }
}

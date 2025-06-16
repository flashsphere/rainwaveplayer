package com.flashsphere.rainwaveplayer.ui.item.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.uistate.model.PreviouslyPlayedSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvPreviouslyPlayed(
    items: List<PreviouslyPlayedSongItem>,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
    showToast: (message: String) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 80.dp, end = 40.dp)) {
        TvPreviouslyPlayedHeader()

        FlowRow(
            modifier = Modifier.focusGroup().fillMaxWidth().padding(vertical = 20.dp),
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { i, item ->
                key(i) {
                    TvPreviouslyPlayedItem(
                        modifier = Modifier.saveLastFocused("previously_played_$i"),
                        item = item,
                        onMoreClick = onMoreClick,
                        showToast = showToast,
                    )
                }
            }
        }
    }
}

@Composable
fun TvPreviouslyPlayedHeader(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Text(text = stringResource(id = R.string.previously_played).uppercase())
    }
}

@Composable
fun TvPreviouslyPlayedItem(
    modifier: Modifier = Modifier,
    item: PreviouslyPlayedSongItem,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
    showToast: (message: String) -> Unit,
) {
    val isLoggedIn = LocalUserCredentials.current.isLoggedIn()
    val context = LocalContext.current

    val onClick = {
        if (isLoggedIn) {
            onMoreClick(item)
        } else {
            showToast(context.getString(R.string.error_not_logged_in))
        }
    }

    TvStationInfoSongCard(
        modifier = modifier,
        item = item,
        onClick = onClick,
        onLongClick = onClick,
    )
}

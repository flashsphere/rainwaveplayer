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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUp
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem

var longPressHintShown = false

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvComingUp(
    index: Int,
    comingUp: ComingUp,
    onVoteClick: (item: ComingUpSongItem) -> Unit,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
    showToast: (message: String) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 80.dp, end = 40.dp)) {
        TvComingUpHeader(item = comingUp.header)

        FlowRow(
            modifier = Modifier
                .focusGroup()
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            comingUp.items.forEachIndexed { i, item ->
                key(i) {
                    TvComingUpItem(
                        modifier = Modifier.saveLastFocused("coming_up_${index}_$i"),
                        item = item,
                        onVoteClick = onVoteClick,
                        onMoreClick = onMoreClick,
                        showToast = showToast,
                    )
                }
            }
        }
    }
}

@Composable
fun TvComingUpHeader(
    modifier: Modifier = Modifier,
    item: ComingUpHeaderItem,
) {
    val eventName = item.getEventName(LocalContext.current, LocalUserCredentials.current.isLoggedIn())
    Box(modifier = modifier) {
        Text(text = eventName.uppercase())
    }
}

@Composable
fun TvComingUpItem(
    modifier: Modifier = Modifier,
    item: ComingUpSongItem,
    onVoteClick: (item: ComingUpSongItem) -> Unit,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
    showToast: (message: String) -> Unit,
) {
    val isLoggedIn = LocalUserCredentials.current.isLoggedIn()
    val context = LocalContext.current

    TvStationInfoSongCard(
        modifier = modifier,
        item = item,
        onClick = {
            if (isLoggedIn) {
                val song = item.data.song
                if (song.votingAllowed && !song.voted.value) {
                    onVoteClick(item)
                    if (!longPressHintShown) {
                        longPressHintShown = true
                        showToast(context.getString(R.string.tv_song_long_press_hint))
                    }
                } else {
                    onMoreClick(item)
                }
            } else {
                showToast(context.getString(R.string.error_not_logged_in))
            }
        },
        onLongClick = {
            if (isLoggedIn) {
                longPressHintShown = true
                onMoreClick(item)
            } else {
                showToast(context.getString(R.string.error_not_logged_in))
            }
        },
    )
}

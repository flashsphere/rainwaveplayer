package com.flashsphere.rainwaveplayer.ui.rating.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.alertdialog.TvCustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.rating.Rating
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import com.flashsphere.rainwaveplayer.view.uistate.model.IdName

@Composable
fun TvRatingDialog(
    ratingState: MutableState<RatingState?>,
    onRate: (ratingState: RatingState) -> Unit,
) {
    val ratingStateValue = ratingState.value ?: return
    val updatedRatingState = rememberSaveable { mutableStateOf(ratingStateValue) }
    val rateButtonEnabled = remember { mutableStateOf(false) }

    val dismissDialog = { ratingState.value = null }

    LaunchedEffect(updatedRatingState.value.rating) {
        val updatedRatingValue = updatedRatingState.value.rating
        rateButtonEnabled.value = updatedRatingValue >= 1 && ratingStateValue.rating != updatedRatingValue
    }

    TvCustomAlertDialog(
        onDismissRequest = dismissDialog,
        title = {
            Text(text = stringResource(id = R.string.rate_song, ratingStateValue.songTitle))
        },
        content = {
            Rating(
                modifier = Modifier.align(Alignment.Center),
                ratingState = updatedRatingState,
                onEnterPress = {
                    if (ratingStateValue.rating != updatedRatingState.value.rating) {
                        onRate(updatedRatingState.value)
                    }
                    dismissDialog()
                }
            )
        },
        buttons = null,
    )
}

@PreviewTv
@Composable
private fun TvRatingDialogPreview() {
    PreviewTvTheme {
        TvRatingDialog(ratingState = remember {
            mutableStateOf(
                RatingState(
                    song = IdName(1, "Song title"),
                    rating = 3.5F,
                )
            )
        }, onRate = {})
    }
}

package com.flashsphere.rainwaveplayer.ui.rating

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.view.uistate.RatingState

@Composable
fun RatingDialog(
    ratingState: MutableState<RatingState?>,
    onRemoveRating: (ratingState: RatingState) -> Unit,
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

    CustomAlertDialog(
        onDismissRequest = dismissDialog,
        title = {
            Text(text = stringResource(id = R.string.rate_song, ratingStateValue.songTitle))
        },
        content = {
            Rating(
                modifier = Modifier.align(Alignment.Center),
                ratingState = updatedRatingState,
            )
        },
        buttons = {
            if (ratingStateValue.rating > 0F) {
                TextButton(onClick = {
                    onRemoveRating(updatedRatingState.value.copy(rating = 0F))
                    dismissDialog()
                }) {
                    Text(text = stringResource(id = R.string.action_remove))
                }
                Spacer(Modifier.weight(1F))
            }
            TextButton(onClick = dismissDialog) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
            TextButton(enabled = rateButtonEnabled.value, onClick = {
                onRate(updatedRatingState.value)
                dismissDialog()
            }) {
                Text(text = stringResource(id = R.string.action_rate))
            }
        },
    )
}

@Preview
@Composable
private fun RatingDialogPreview() {
    PreviewTheme {
        RatingDialog(ratingState = remember {
            mutableStateOf(
                RatingState(
                    songId = 1,
                    songTitle = "Song title",
                    rating = 3.5F,
                )
            )
        }, {}, {})
    }
}

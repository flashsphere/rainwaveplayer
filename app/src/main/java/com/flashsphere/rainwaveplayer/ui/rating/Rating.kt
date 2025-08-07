package com.flashsphere.rainwaveplayer.ui.rating

import android.content.res.ColorStateList
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.widget.RatingBar
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import androidx.appcompat.R as AppCompatR

@Composable
fun Rating(
    modifier: Modifier,
    ratingState: MutableState<RatingState>,
    onEnterPress: (() -> Unit)? = null,
) {
    AndroidView(
        factory = { ctx ->
            RatingBar(
                ctx,
                null,
                AppCompatR.attr.ratingBarStyle,
                AppCompatR.style.Widget_AppCompat_RatingBar
            ).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                progressTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.md_theme_secondary))
                secondaryProgressTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.md_theme_secondary))
                progressBackgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.md_theme_surfaceVariant))
                numStars = 5
                stepSize = 0.5F
                rating = ratingState.value.rating
                setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                    if (!fromUser) return@setOnRatingBarChangeListener
                    val previousRating = ratingState.value.rating
                    if (rating > 0F && rating < 1F) {
                        if (previousRating < rating) {
                            ratingBar.dispatchKeyEvent(KeyEvent(ACTION_DOWN, KEYCODE_DPAD_RIGHT))
                        } else {
                            ratingBar.dispatchKeyEvent(KeyEvent(ACTION_DOWN, KEYCODE_DPAD_LEFT))
                        }
                    } else {
                        ratingState.value = ratingState.value.copy(rating = rating)
                    }
                }
                if (onEnterPress != null) {
                    setOnKeyListener(object : View.OnKeyListener {
                        override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                            if (event.action != ACTION_DOWN) return false
                            if (event.keyCode != KEYCODE_ENTER && event.keyCode != KEYCODE_DPAD_CENTER) return false
                            onEnterPress()
                            return true
                        }
                    })
                }
            }
        },
        update = { ratingBar ->
            ratingBar.rating = ratingState.value.rating
        },
        onRelease = {
            it.onRatingBarChangeListener = null
            it.setOnKeyListener(null)
        },
        modifier = modifier.wrapContentSize(),
    )
}

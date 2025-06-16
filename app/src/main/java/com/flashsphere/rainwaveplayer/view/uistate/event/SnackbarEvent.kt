package com.flashsphere.rainwaveplayer.view.uistate.event

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.SnackbarState
import com.flashsphere.rainwaveplayer.ui.SnackbarStateData
import com.flashsphere.rainwaveplayer.ui.SnackbarStateType
import com.flashsphere.rainwaveplayer.ui.toSnackbarData
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState

interface SnackbarEvent : UiEvent {
    fun toSnackbarState(context: Context): SnackbarState
}

object DismissSnackbarEvent: SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(type = SnackbarStateType.Dismiss)
}

object UserNotLoggedInEvent : SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = SnackbarStateData(
            message = context.getString(R.string.error_not_logged_in),
            duration = SnackbarDuration.Long,
        )
    )
}

@Immutable
class RequestSongSuccessEvent(
    val song: SongState,
): SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = SnackbarStateData(
            message = context.getString(R.string.request_success),
            duration = SnackbarDuration.Long,
        )
    )
}

@Immutable
class RequestSongErrorEvent(
    val song: SongState,
    val error: OperationError,
    val retry: (() -> Unit),
): SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = error.toSnackbarData(context,
            context.getString(R.string.error_request_song_failed),
            SnackbarDuration.Long,
            retry)
    )
}

@Immutable
class FaveSongErrorEvent(
    val songId: Int,
    val favorite: Boolean,
    val error: OperationError,
    val retry: (() -> Unit)?,
): SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = error.toSnackbarData(context,
            if (favorite) {
                context.getString(R.string.error_unfave_failed)
            } else {
                context.getString(R.string.error_fave_failed)
            },
            SnackbarDuration.Long,
            retry)
    )
}

@Immutable
class FaveAlbumErrorEvent(
    val album: AlbumState,
    val error: OperationError,
    val retry: (() -> Unit),
): SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = error.toSnackbarData(context,
            if (album.favorite.value) {
                context.getString(R.string.error_unfave_failed)
            } else {
                context.getString(R.string.error_fave_failed)
            },
            SnackbarDuration.Long,
            retry)
    )
}

@Immutable
class RequestErrorEvent(
    val error: OperationError,
    val retry: (() -> Unit),
    @StringRes val message: Int,
) : SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = error.toSnackbarData(context,
            context.getString(message),
            SnackbarDuration.Long,
            retry)
    )
}

@Immutable
class VoteSongErrorEvent(
    val error: OperationError,
    val retry: (() -> Unit),
) : SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = error.toSnackbarData(context,
            context.getString(R.string.error_voting),
            SnackbarDuration.Long,
            retry)
    )
}

@Immutable
class RateSongSuccessEvent(
    val message: String?
) : SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = SnackbarStateData(
            message = message ?: context.getString(R.string.rating_success),
            duration = SnackbarDuration.Long,
        )
    )
}

@Immutable
class RateSongErrorEvent(
    val error: OperationError,
    val retry: (() -> Unit),
) : SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = error.toSnackbarData(context,
            context.getString(R.string.error_rate_failed),
            SnackbarDuration.Long,
            retry)
    )
}

@Immutable
class MessageEvent(
    val message: String,
    val retry: (() -> Unit)? = null,
): SnackbarEvent {
    override fun toSnackbarState(context: Context) = SnackbarState(
        type = SnackbarStateType.Show,
        data = SnackbarStateData(
            message = message,
            duration = SnackbarDuration.Long,
            actionLabel = context.getString(R.string.action_retry),
            action = retry,
        )
    )
}

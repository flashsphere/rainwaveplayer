package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.CoilImage
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState

@Composable
fun UserAvatar(modifier: Modifier = Modifier, size: Dp = 50.dp, user: UserState?) {
    if (user != null && !user.isAnon()) {
        if (user.avatar.isNullOrBlank()) {
            Image(painter = painterResource(id = R.drawable.ic_account_circle_white_50dp),
                contentDescription = null, modifier = modifier.size(size))
        } else {
            CoilImage(
                image = user.avatar,
                contentScale = ContentScale.Fit,
                fallback = painterResource(id = R.drawable.ic_account_circle_white_50dp),
                error = painterResource(id = R.drawable.ic_account_circle_white_50dp),
                placeholder = if (LocalInspectionMode.current) painterResource(R.drawable.ic_account_circle_white_50dp) else null,
                modifier = modifier.size(size).clip(CircleShape),
            )
        }
    } else {
        Spacer(modifier = modifier.size(size))
    }
}

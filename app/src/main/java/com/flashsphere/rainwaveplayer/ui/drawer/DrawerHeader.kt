package com.flashsphere.rainwaveplayer.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.item.UserAvatar
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.userStateData
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DrawerHeader(
    userFlow: StateFlow<UserState?>,
    onLoginClick: () -> Unit
) {
    val user = userFlow.collectAsStateWithLifecycle().value

    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .background(MaterialTheme.colorScheme.primaryContainer)
        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
        .clickable(enabled = !LocalUserCredentials.current.isLoggedIn(), onClick = onLoginClick)
        .padding(vertical = 16.dp, horizontal = 28.dp)
    ) {
        UserAvatar(user = user)
        if (user != null && !user.isAnon()) {
            Text(text = user.name, style = AppTypography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp))
        } else if (!LocalUserCredentials.current.isLoggedIn()) {
            Text(text = stringResource(id = R.string.login), style = AppTypography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp))
        } else {
            Text(text = stringResource(id = R.string.loading), style = AppTypography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp))
        }
    }
}

@Preview
@Composable
private fun DrawerHeaderPreview(
    @PreviewParameter(DrawerHeaderPreviewParameterProvider::class) params: Pair<UserCredentials?, UserState?>,
) {
    PreviewTheme(userCredentials = params.first) {
        Surface {
            DrawerHeader(
                userFlow = MutableStateFlow(params.second),
                onLoginClick = {}
            )
        }
    }
}

private class DrawerHeaderPreviewParameterProvider : PreviewParameterProvider<Pair<UserCredentials?, UserState?>> {
    override val values: Sequence<Pair<UserCredentials?, UserState?>> = sequenceOf(
        Pair(
            UserCredentials(2, ""),
            userStateData[1],
        ),
        Pair(UserCredentials(2, ""), null),
        Pair(
            null,
            userStateData[0],
        )
    )
}

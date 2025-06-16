package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.WebView
import com.flashsphere.rainwaveplayer.ui.item.tv.TvTextButton
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTheme
import com.flashsphere.rainwaveplayer.view.helper.Urls

@Composable
fun TvWebViewScreen(
    url: String,
    onCloseClick: () -> Unit,
) {
    val pageTitle = remember { mutableStateOf(Urls.LOGIN_URL.toString()) }
    TvAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.imePadding().fillMaxSize()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvTextButton(onClick = onCloseClick) {
                        Text(text = stringResource(R.string.action_close))
                    }
                    Spacer(Modifier.size(16.dp))
                    Text(modifier = Modifier.weight(1F), text = pageTitle.value,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                WebView(
                    modifier = Modifier.weight(1F),
                    url = url,
                    pageTitle = pageTitle,
                )
            }
        }
    }
}

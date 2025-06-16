package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.CloseIcon
import com.flashsphere.rainwaveplayer.ui.WebView
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    onCloseClick: () -> Unit,
) {
    val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    val pageTitle = remember { mutableStateOf(url) }
    AppTheme {
        AppScaffold(
            modifier = Modifier.windowInsetsPadding(windowInsets),
            appBarContent = {
                AppBarTitle(title = pageTitle.value)
            },
            navigationIcon = {
                CloseIcon(onCloseClick = onCloseClick)
            },
        ) {
            WebView(
                modifier = Modifier.fillMaxSize(),
                url = url,
                pageTitle = pageTitle,
            )
        }
    }
}

package com.flashsphere.rainwaveplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import com.flashsphere.rainwaveplayer.view.fragment.WebViewFragment
import timber.log.Timber

@Composable
fun WebView(
    modifier: Modifier,
    url: String,
    pageTitle: MutableState<String>,
) {
    AndroidFragment<WebViewFragment>(
        arguments = bundleOf(WebViewFragment.ARG_URL to url),
        modifier = modifier,
        onUpdate = { fragment ->
            Timber.d("onUpdate")
            fragment.pageTitleChangedCallback = { pageTitle.value = it }
        }
    )
}

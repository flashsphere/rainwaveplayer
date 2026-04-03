package com.flashsphere.rainwaveplayer.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
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
        arguments = Bundle().also { it.putString(WebViewFragment.ARG_URL, url) },
        modifier = modifier,
        onUpdate = { fragment ->
            Timber.d("onUpdate")
            fragment.pageTitleChangedCallback = { pageTitle.value = it }
        }
    )
}

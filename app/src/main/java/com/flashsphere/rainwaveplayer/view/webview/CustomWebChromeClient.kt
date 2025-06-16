package com.flashsphere.rainwaveplayer.view.webview

import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.widget.ContentLoadingProgressBar

class CustomWebChromeClient(
    private val progressBar: ContentLoadingProgressBar
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        progressBar.progress = newProgress
        if (newProgress == 100) {
            progressBar.hide()
        } else {
            progressBar.show()
        }
    }
}

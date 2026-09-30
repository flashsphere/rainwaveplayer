package com.flashsphere.rainwaveplayer.view.webview

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat

class CustomWebViewClient(
    private val callback: Callback,
) : WebViewClientCompat() {

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        callback.pageTitleChanged(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        callback.pageTitleChanged(view.title ?: url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        callback.doUpdateVisitedHistory(view, url, isReload)
    }

    @Deprecated("Deprecated in WebViewClient")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return shouldOverrideUrlLoading(url)
    }

    private fun shouldOverrideUrlLoading(url: String): Boolean {
        return callback.shouldOverrideUrlLoading(url)
    }

    interface Callback {
        fun pageTitleChanged(title: String) = run {}
        fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) = run {}
        fun shouldOverrideUrlLoading(url: String): Boolean = run { return false }
    }
}

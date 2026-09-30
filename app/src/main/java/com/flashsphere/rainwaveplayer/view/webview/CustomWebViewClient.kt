package com.flashsphere.rainwaveplayer.view.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewClientCompat

@SuppressLint("MissingOnRenderProcessGone") // https://issuetracker.google.com/issues/548989591
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        return callback.onRenderProcessGone(view, detail)
    }

    interface Callback {
        fun pageTitleChanged(title: String) = run {}
        fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) = run {}
        fun shouldOverrideUrlLoading(url: String): Boolean = run { return false }
        fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean
    }
}

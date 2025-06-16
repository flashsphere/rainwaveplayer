package com.flashsphere.rainwaveplayer.view.activity.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvWebViewScreen
import com.flashsphere.rainwaveplayer.view.activity.BaseActivity
import com.flashsphere.rainwaveplayer.view.helper.Urls
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvWebViewActivity : BaseActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            finish()
            return
        }

        setContent("TvWebViewScreen", bundle = Bundle().also {
            it.putString("url", url)
        }) {
            val configuration = LocalConfiguration.current
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(
                LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass)
            ) {
                TvWebViewScreen(url = url, onCloseClick = { finish() })
            }
        }
    }

    companion object {
        private const val EXTRA_URL = "extra.url"

        fun startActivity(context: Context, url: Uri) {
            val intent = Intent(context, TvWebViewActivity::class.java)
            intent.putExtra(EXTRA_URL, url.toString())
            context.startActivity(intent)
        }
        fun startActivityForLogin(context: Context) {
            startActivity(context, Urls.LOGIN_URL)
        }
    }
}

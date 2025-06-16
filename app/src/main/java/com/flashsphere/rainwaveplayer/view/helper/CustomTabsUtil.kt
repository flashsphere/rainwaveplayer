package com.flashsphere.rainwaveplayer.view.helper

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.view.activity.WebViewActivity
import com.flashsphere.rainwaveplayer.view.helper.CustomTabsHelper.getPackageNameToUse
import com.flashsphere.rainwaveplayer.view.helper.Urls.DISCORD_URL
import com.flashsphere.rainwaveplayer.view.helper.Urls.LOGIN_URL
import com.flashsphere.rainwaveplayer.view.helper.Urls.PATREON_URL

object CustomTabsUtil {
    fun openLoginPage(context: Context) {
        launchWebView(context, LOGIN_URL)
    }

    fun openPatreonPage(context: Context) {
        runCatching {
            context.startActivity(Intent(ACTION_VIEW, PATREON_URL))
        }.onFailure {
            launchWebView(context, PATREON_URL)
        }
    }

    fun openDiscordPage(context: Context) {
        runCatching {
            context.startActivity(Intent(ACTION_VIEW, DISCORD_URL))
        }.onFailure {
            launchWebView(context, DISCORD_URL)
        }
    }

    fun openCustomTab(context: Context, url: Uri) {
        val packageName = getPackageNameToUse(context)
        if (packageName.isNullOrBlank()) {
            launchWebView(context, url)
            return
        }

        runCatching {
            launchCustomTab(context, packageName, url)
        }.onFailure {
            launchWebView(context, url)
        }
    }

    private fun launchCustomTab(context: Context, packageName: String, url: Uri) {
        val params = CustomTabColorSchemeParams.Builder()
            .setNavigationBarColor(ContextCompat.getColor(context, R.color.md_theme_background))
            .setToolbarColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))
            .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .build()

        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.launchUrl(context, url)
    }

    private fun launchWebView(context: Context, url: Uri) {
        WebViewActivity.startActivity(context, url)
    }
}

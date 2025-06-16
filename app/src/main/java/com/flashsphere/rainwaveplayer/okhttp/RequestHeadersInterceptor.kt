package com.flashsphere.rainwaveplayer.okhttp

import android.content.Context
import android.os.Build
import com.flashsphere.rainwaveplayer.BuildConfig
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestHeadersInterceptor @Inject constructor(
    @ApplicationContext val context: Context,
) : Interceptor {
    private val userAgent = buildUserAgentString(context)
    private val language = Locale.getDefault().language

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val httpUrl = request.url
        val url = httpUrl.toUrl().toString()

        val requestBuilder: Request.Builder = chain.request()
            .newBuilder()

        if (url.startsWith(RainwaveService.BASE_URL)) {
            requestBuilder
                .removeHeader(HEADER_USER_AGENT)
                .addHeader(HEADER_USER_AGENT, userAgent)
        }

        if (url.startsWith(RainwaveService.API_URL)) {
            requestBuilder
                .addHeader(HEADER_ACCEPT_LANGUAGE, language)
        }

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"

        private fun buildUserAgentString(context: Context): String {
            val versionName = runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                info.versionName
            }.getOrElse { "?" }
            return (context.getString(R.string.app_name) + "/" + versionName
                + " (Linux;Android " + Build.VERSION.RELEASE + ") "
                + "OkHttp3/" + BuildConfig.OKHTTP_VERSION)
        }
    }
}

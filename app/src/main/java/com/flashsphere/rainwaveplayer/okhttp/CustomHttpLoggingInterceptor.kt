package com.flashsphere.rainwaveplayer.okhttp

import com.flashsphere.rainwaveplayer.repository.RainwaveService
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class CustomHttpLoggingInterceptor : Interceptor {
    private val defaultLoggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE)
    private val apiLoggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val httpUrl = request.url
        val url = httpUrl.toUrl().toString()

        if (url.startsWith(RainwaveService.API_URL)) {
            return apiLoggingInterceptor.intercept(chain)
        }
        if (url.startsWith(RainwaveService.BASE_URL)) {
            return defaultLoggingInterceptor.intercept(chain)
        }

        return chain.proceed(request)
    }
}

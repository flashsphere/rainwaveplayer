package com.flashsphere.rainwaveplayer.okhttp

import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.repository.UserRepository
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticatedUserInterceptor
@Inject constructor(
    private val userRepository: UserRepository,
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        var httpUrl = request.url
        val url = httpUrl.toUrl().toString()

        val userCredentials = userRepository.getCredentials()

        if (url.startsWith(RainwaveService.API_URL) && !url.contains("/user_info") && userCredentials != null) {
            val requestBuilder = request.newBuilder()

            when (request.method.lowercase()) {
                "get" -> {
                    httpUrl = request.url.newBuilder()
                        .addQueryParameter("user_id", userCredentials.userId.toString())
                        .addQueryParameter("key", userCredentials.apiKey)
                        .build()
                    requestBuilder.url(httpUrl)
                }
                "post" -> {
                    var body = request.body
                    if (body != null) {
                        if (body.contentLength() == 0L) {
                            body = FormBody.Builder().build()
                        }
                        if (body is FormBody) {
                            val originalFormBody = body
                            val formBuilder = FormBody.Builder()

                            formBuilder
                                .add("user_id", userCredentials.userId.toString())
                                .add("key", userCredentials.apiKey)

                            var i = 0
                            val size: Int = originalFormBody.size
                            while (i < size) {
                                formBuilder.addEncoded(originalFormBody.encodedName(i), originalFormBody.encodedValue(i))
                                i++
                            }
                            requestBuilder.post(formBuilder.build())
                        }
                    }
                }
            }
            request = requestBuilder.build()
        }
        return chain.proceed(request)
    }
}

package com.flashsphere.rainwaveplayer.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateExpiredException

class CrashlyticsTree(
    @ApplicationContext private val context: Context,
    coroutineDispatchers: CoroutineDispatchers,
    dataStore: DataStore<Preferences>,
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    }

    companion object {
        private val exceptionsToNotLog = setOf(
            UnknownHostException::class.java,
            SocketTimeoutException::class.java,
            ConnectException::class.java,
            CertificateExpiredException::class.java
        )

        fun shouldLogException(throwable: Throwable): Boolean {
            var rootCause: Throwable = throwable
            if (exceptionsToNotLog.contains(rootCause::class.java)) {
                return false
            }
            if (rootCause is HttpException && (rootCause.code() == 502 || rootCause.code() == 403)) {
                return false
            }
            while (rootCause.cause != null && rootCause.cause != rootCause) {
                rootCause = rootCause.cause!!
                if (exceptionsToNotLog.contains(rootCause::class.java)) {
                    return false
                }
            }
            return true
        }
    }
}

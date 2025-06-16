package com.flashsphere.rainwaveplayer.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.CRASH_REPORTING
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

    private val scope = coroutineDispatchers.scope
    private var crashReportingEnabled = dataStore.getBlocking(CRASH_REPORTING)

    init {
        configureCrashlytics(crashReportingEnabled)
        dataStore.data
            .map { preferences -> preferences[CRASH_REPORTING.key] ?: CRASH_REPORTING.defaultValue }
            .distinctUntilChanged()
            .onEach { onSettingChanged(it) }
            .launchWithDefaults(scope, "Crash Reporting setting changed")
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        if (!crashReportingEnabled) {
            Timber.d("Crash reporting not enabled!")
            return
        }

        scope.launchWithDefaults("Crashlytics") {
            val loggedMessage = StringBuilder()
                .apply {
                    if (!tag.isNullOrBlank()) {
                        append(tag).append(": ")
                    }
                }
                .append(message)
                .toString()

            Firebase.crashlytics.log(loggedMessage)

            t?.let { logException(it) }
        }
    }

    private fun logException(throwable: Throwable) {
        if (!shouldLogException(throwable)) {
            return
        }
        Firebase.crashlytics.recordException(throwable)
    }

    private fun onSettingChanged(crashReportingEnabled: Boolean) {
        if (this.crashReportingEnabled != crashReportingEnabled) {
            this.crashReportingEnabled = crashReportingEnabled
            configureCrashlytics(crashReportingEnabled)
        }
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

        private fun configureCrashlytics(enabled: Boolean) {
            Timber.d("Crashlytics enabled = %s", enabled)
            Firebase.crashlytics.isCrashlyticsCollectionEnabled = enabled
            if (!enabled) {
                Firebase.crashlytics.deleteUnsentReports()
            }
        }
    }
}

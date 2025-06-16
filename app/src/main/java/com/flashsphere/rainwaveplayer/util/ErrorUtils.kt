package com.flashsphere.rainwaveplayer.util

import android.os.Build
import androidx.media3.datasource.HttpDataSource
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.io.InterruptedIOException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateExpiredException
import javax.net.ssl.SSLPeerUnverifiedException

object ErrorUtils {
    private fun <T : HasResponseResult<*>> extractResponseResult(e: HttpException, converter: Converter<ResponseBody, T>): ResponseResult? {
        runCatching {
            e.response()?.errorBody()?.use { responseBody ->
                return converter.convert(responseBody)?.result
            }
        }
        return null
    }

    fun <T : HasResponseResult<*>> extractError(e: Throwable, converter: Converter<ResponseBody, T>?): OperationError {
        if (e is IOException) {
            return OperationError(OperationError.Connectivity, e.message)
        }
        if (e is HttpException && converter != null) {
            val responseResult = extractResponseResult(e, converter)
            if (responseResult != null) {
                return if (responseResult.code == 403) {
                    OperationError(OperationError.Unauthorized, responseResult.text)
                } else {
                    OperationError(OperationError.Server, responseResult.text)
                }
            }
        }
        return OperationError(OperationError.Unknown)
    }

    fun getRootCause(t: Throwable): Throwable {
        var rootCause = t
        while (rootCause.cause != null && rootCause.cause != rootCause) {
            rootCause = rootCause.cause!!
        }
        return rootCause
    }

    fun isRetryable(cause: Throwable, connectivityObserver: ConnectivityObserver): Boolean {
        if (cause is IOException &&
            cause::class != InterruptedIOException::class &&
            connectivityObserver.isConnected()) {
            return false
        }
        // don't retry on certificate errors
        if (cause is SSLPeerUnverifiedException &&
            connectivityObserver.isConnected()) {
            return false
        }
        // don't retry on HTTP 400+ errors
        if (cause is HttpException) {
            if (cause.code() >= 400) {
                Timber.d("http %d exception", cause.code())
                return false
            }
        }

        val rootCause = getRootCause(cause)
        if (rootCause is CertificateExpiredException) {
            return false
        }
        if (rootCause is CertPathValidatorException) {
            Timber.i("CertPathValidatorException index: %d", rootCause.index)
            Timber.i("CertPathValidatorException certPath: %s", rootCause.certPath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Timber.i("CertPathValidatorException reason: %s", rootCause.reason)
            }
            return false
        }
        if (rootCause is HttpDataSource.InvalidResponseCodeException && rootCause.responseCode >= 400) {
            return false
        }
        return true
    }
}

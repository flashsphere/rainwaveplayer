package com.flashsphere.rainwaveplayer.util

import android.net.http.SslError
import android.net.http.SslError.SSL_UNTRUSTED
import android.os.Build
import com.flashsphere.rainwaveplayer.okhttp.TrustedCertificateStore
import timber.log.Timber

object WebViewSslErrorHandler {
    fun validateSslCertificate(trustedCertificateStore: TrustedCertificateStore,
                               error: SslError): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            var validCert = false
            when (error.primaryError) {
                SSL_UNTRUSTED -> {
                    Timber.d("The certificate authority '%s' is not trusted",
                        error.certificate.issuedBy.dName)
                    validCert = trustedCertificateStore.validate(error.certificate)
                }
                else -> Timber.d("SSL error %s", error.primaryError)
            }
            return validCert
        }
        return false
    }
}

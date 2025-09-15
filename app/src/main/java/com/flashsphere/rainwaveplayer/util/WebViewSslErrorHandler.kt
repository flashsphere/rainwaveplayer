package com.flashsphere.rainwaveplayer.util

import android.net.http.SslError
import android.net.http.SslError.SSL_UNTRUSTED
import com.flashsphere.rainwaveplayer.okhttp.TrustedCertificateStore
import timber.log.Timber

interface WebViewSslErrorHandler {
    fun validateSslCertificate(error: SslError): Boolean
}

class Api21WebViewSslErrorHandler(
    private val trustedCertificateStore: TrustedCertificateStore
) : WebViewSslErrorHandler {
    override fun validateSslCertificate(error: SslError): Boolean {
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
}

class NoOpWebViewSslErrorHandler : WebViewSslErrorHandler {
    override fun validateSslCertificate(error: SslError): Boolean {
        return false
    }
}

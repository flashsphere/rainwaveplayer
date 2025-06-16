package com.flashsphere.rainwaveplayer.okhttp

import android.content.Context
import android.net.http.SslCertificate
import androidx.annotation.RawRes
import com.flashsphere.rainwaveplayer.R
import timber.log.Timber
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class TrustedCertificateStore(context: Context) {
    val certificates = listOf(
        TrustedCertificate(context, R.raw.e5, false),
        TrustedCertificate(context, R.raw.e6, false),
        TrustedCertificate(context, R.raw.r10, false),
        TrustedCertificate(context, R.raw.r11, false),
        TrustedCertificate(context, R.raw.isrgrootx1, true),
        TrustedCertificate(context, R.raw.isrgrootx2, true),
    )

    val rootCertificates = certificates.filter { it.root }

    private fun findByCName(cName: String): TrustedCertificate? {
        return certificates.asSequence()
            .filter { SslCertificate(it.certificate as X509Certificate).issuedTo.cName == cName }
            .firstOrNull()
    }

    fun validate(sslCert: SslCertificate): Boolean {
        val cName = sslCert.issuedBy.cName
        val trustedCertificate = findByCName(cName)
        if (trustedCertificate == null) {
            Timber.w("Unable to find certificate trusted anchor for '%s'", sslCert.issuedBy.cName)
            return false
        }

        val result = verifyCertificate(sslCert.toX509Certificate(), trustedCertificate.certificate)
        if (result.isFailure) {
            return false
        }
        if (trustedCertificate.root) {
            return true
        }
        return validate(SslCertificate(trustedCertificate.certificate as X509Certificate))
    }

    private fun verifyCertificate(certificate: Certificate, issuerCertificate: Certificate): Result<Unit> {
        return runCatching {
            if (issuerCertificate is X509Certificate) {
                issuerCertificate.checkValidity()
            }
            certificate.verify(issuerCertificate.publicKey)
        }
    }
}

class TrustedCertificate(
    private val context: Context,
    @RawRes private val resourceId: Int,
    val root: Boolean,
) {
    val certificate: Certificate by lazy {
        Timber.d("Loading certificate resource %d", resourceId)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        context.resources.openRawResource(resourceId).use { inputStream ->
            certificateFactory.generateCertificate(inputStream)
        }
    }
}

fun SslCertificate.toX509Certificate(): X509Certificate {
    val bundle = SslCertificate.saveState(this)
    val bytes = bundle.getByteArray("x509-certificate")
    return CertificateFactory.getInstance("X.509")
        .generateCertificate(bytes?.inputStream()) as X509Certificate
}

package com.flashsphere.rainwaveplayer.media

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.ogg.OggExtractor
import timber.log.Timber

@OptIn(UnstableApi::class)
object MediaExtractorsFactory : ExtractorsFactory {
    override fun createExtractors(): Array<Extractor> {
        return arrayOf(
            mp3Extractor(),
            oggExtractor(),
        )
    }

    override fun createExtractors(
        uri: Uri,
        responseHeaders: Map<String, List<String>>
    ): Array<Extractor> {
        val filename = uri.lastPathSegment ?: return createExtractors()

        if (filename.endsWith(".mp3", ignoreCase = true)) {
            Timber.d("Using mp3 extractor")
            return arrayOf(mp3Extractor())
        }
        if (filename.endsWith(".ogg", ignoreCase = true)) {
            Timber.d("Using ogg extractor")
            return arrayOf(oggExtractor())
        }
        return createExtractors()
    }

    private fun mp3Extractor() = Mp3Extractor(
        Mp3Extractor.FLAG_DISABLE_ID3_METADATA or Mp3Extractor.FLAG_DISABLE_ARTWORK_METADATA
    )

    private fun oggExtractor() = OggExtractor()
}

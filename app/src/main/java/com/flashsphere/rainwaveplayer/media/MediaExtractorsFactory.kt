package com.flashsphere.rainwaveplayer.media

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.ogg.OggExtractor

@OptIn(UnstableApi::class) class Mp3ExtractorFactory : ExtractorsFactory {
    private val extractors = arrayOf<Extractor>(Mp3Extractor(Mp3Extractor.FLAG_DISABLE_ID3_METADATA))

    override fun createExtractors(): Array<Extractor> {
        return extractors
    }
}

@OptIn(UnstableApi::class) class OggExtractorFactory : ExtractorsFactory {
    private val extractors = arrayOf<Extractor>(OggExtractor())

    override fun createExtractors(): Array<Extractor> {
        return extractors
    }
}

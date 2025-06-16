package com.flashsphere.rainwaveplayer.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class AudioFocusManager(
    context: Context,
    private val listener: OnAudioFocusChangeListener,
) {
    private val audioManager: AudioManager = ContextCompat.getSystemService(context.applicationContext, AudioManager::class.java) as AudioManager

    private val streamType = AudioManager.STREAM_MUSIC
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(USAGE_MEDIA)
        .setContentType(CONTENT_TYPE_MUSIC)
        .build()
    private var audioFocusRequest: AudioFocusRequest? = null

    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusApi26()
        } else {
            requestAudioFocusDefault()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusApi26(): Boolean {
        val audioFocusRequest = this.audioFocusRequest ?: AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(listener)
            .build()
            .also { this.audioFocusRequest = it }
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun requestAudioFocusDefault(): Boolean {
        @Suppress("DEPRECATION")
        val result = audioManager.requestAudioFocus(listener, streamType, AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            abandonAudioFocusApi26()
        } else {
            abandonAudioFocusDefault()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocusApi26(): Boolean {
        val audioFocusRequest = this.audioFocusRequest ?: AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(listener)
            .build()
            .also { this.audioFocusRequest = it }
        val result = audioManager.abandonAudioFocusRequest(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocusDefault(): Boolean {
        @Suppress("DEPRECATION")
        val result = audioManager.abandonAudioFocus(listener)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}

package com.flashsphere.rainwaveplayer.cast

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.isTv
import com.google.android.gms.cast.tv.CastReceiverContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastReceiverContextHolder @Inject constructor(
    @ApplicationContext applicationContext: Context,
    mediaPlayerStateObserver: MediaPlayerStateObserver,
) {
    val context: CastReceiverContext? = if (applicationContext.isTv()) {
        runCatching {
            CastReceiverContext.initInstance(applicationContext)
            CastReceiverContext.getInstance()
                .apply {
                    ProcessLifecycleOwner.get().lifecycle.addObserver(
                        CastReceiverProcessLifecycleObserver(this, mediaPlayerStateObserver))
                    registerEventCallback(object : CastReceiverContext.EventCallback() {
                        override fun onStopApplication() {
                            MediaService.stop(applicationContext)
                        }
                    })
                }
        }.getOrNull()
    } else {
        null
    }
}

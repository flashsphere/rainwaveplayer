package com.flashsphere.rainwaveplayer.cast

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.google.android.gms.cast.tv.CastReceiverContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class CastReceiverProcessLifecycleObserver(
    private val castReceiverContext: CastReceiverContext,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
) : DefaultLifecycleObserver {
    private var background = true
    private var job: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        Timber.d("onStart")
        background = false
        castReceiverContext.start()

        cancel(job)
        job = mediaPlayerStateObserver.flow
            .filter { it.isStopped() }
            .onEach { stopCastReceiverContextIfNeeded() }
            .launchIn(owner.lifecycleScope)
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("onStop")
        background = true
        stopCastReceiverContextIfNeeded()
    }

    private fun stopCastReceiverContextIfNeeded() {
        if (background && mediaPlayerStateObserver.currentState.isStopped()) {
            Timber.d("stop cast receiver")
            castReceiverContext.stop()
        }
    }
}

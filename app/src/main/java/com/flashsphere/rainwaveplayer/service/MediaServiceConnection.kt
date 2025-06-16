package com.flashsphere.rainwaveplayer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

class MediaServiceConnection(
    private val context: Context,
    private val serviceConnected: MediaServiceConnected,
) : ServiceConnection, DefaultLifecycleObserver {
    private val serviceComponent = ComponentName(context, MediaService::class.java)
    private var boundService: MediaService.LocalBinder? = null

    override fun onStart(owner: LifecycleOwner) {
        runCatching {
            val intent = Intent(context, MediaService::class.java)
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        }.onFailure {
            Timber.e(it, "Can't start/bind service")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (boundService == null) return
        runCatching {
            Timber.i("Unbind service")
            context.unbindService(this)
            onServiceDisconnected(serviceComponent)
        }.onFailure {
            Timber.e(it, "Can't unbind service")
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
        if (serviceComponent != name) return
        if (binder !is MediaService.LocalBinder) return
        Timber.i("MediaService connected")
        boundService = binder
        serviceConnected.serviceConnected(binder)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        if (serviceComponent != name) return
        boundService = null
        serviceConnected.serviceDisconnected()
    }

    interface MediaServiceConnected {
        fun serviceConnected(binder: MediaService.LocalBinder)
        fun serviceDisconnected()
    }
}

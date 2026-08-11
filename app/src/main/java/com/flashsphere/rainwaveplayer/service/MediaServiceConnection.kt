package com.flashsphere.rainwaveplayer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

@Singleton
class MediaServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) : ServiceConnection, DefaultLifecycleObserver {
    private val serviceComponent = ComponentName(context, MediaService::class.java)

    val boundService: StateFlow<MediaService?>
        field = MutableStateFlow(null)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        runCatching {
            val intent = Intent(context, MediaService::class.java)
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        }.onFailure {
            Timber.e(it, "Can't start/bind service")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (boundService.value == null) return
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
        boundService.value = binder.service.get()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        if (serviceComponent != name) return
        boundService.value = null
    }
}

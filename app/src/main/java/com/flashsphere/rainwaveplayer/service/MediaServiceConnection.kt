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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) : ServiceConnection, DefaultLifecycleObserver {
    private val serviceComponent = ComponentName(context, MediaService::class.java)

    private val _boundService = MutableStateFlow<MediaService.LocalBinder?>(null)
    val boundService = _boundService.asStateFlow()

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
        if (_boundService.value == null) return
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
        _boundService.value = binder
    }

    override fun onServiceDisconnected(name: ComponentName) {
        if (serviceComponent != name) return
        _boundService.value = null
    }
}

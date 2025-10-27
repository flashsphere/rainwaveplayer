package com.flashsphere.rainwaveplayer.internal.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.util.PreferencesKeys
import timber.log.Timber

class SettingsMigration(private val context: Context) : DataMigration<Preferences> {
    override suspend fun cleanUp() {
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[PreferencesKeys.VERSION.key]
        return version == null || version < PreferencesKeys.VERSION.defaultValue
    }

    @Suppress("DEPRECATION")
    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutablePrefs = currentData.toMutablePreferences()

        val version = mutablePrefs[PreferencesKeys.VERSION.key] ?: PreferencesKeys.VERSION.defaultValue
        Timber.d("Migrating settings version %d", version)

        if (version <= 6) {
            mutablePrefs.remove(PreferencesKeys.BUFFER_MAX)
            mutablePrefs.remove(PreferencesKeys.BUFFER_FOR_PLAYBACK)
            mutablePrefs.remove(PreferencesKeys.BUFFER_REBUFFER)
        }
        if (version <= 7) {
            // clear existing Glide cache due to migrating from Glide to Coil for image loading
            runCatching {
                context.cacheDir.resolve("image_manager_disk_cache").deleteRecursively()
            }.onFailure {
                Timber.w(it, "Error removing Glide image cache")
            }
        }
        if (version <= 8) {
            mutablePrefs.remove(PreferencesKeys.SYSTEM_RESUMPTION)
        }
        if (version <= 9) {
            mutablePrefs.remove(PreferencesKeys.CRASH_REPORTING)
            mutablePrefs.remove(PreferencesKeys.ANALYTICS)
        }

        mutablePrefs[PreferencesKeys.VERSION.key] = PreferencesKeys.VERSION.defaultValue
        return mutablePrefs.toPreferences()
    }
}

package com.flashsphere.rainwaveplayer.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.flashsphere.rainwaveplayer.model.station.Station
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

fun DataStore<Preferences>.getBufferInMillis(): Int {
    val value = getBlocking(PreferencesKeys.BUFFER_MIN)
    return value.toDouble().seconds.inWholeMilliseconds.toInt()
}

fun DataStore<Preferences>.listenUsingOgg(): Boolean {
    return getBlocking(PreferencesKeys.USE_OGG)
}

fun DataStore<Preferences>.getLastPlayedStation(stations: List<Station>): Station {
    val lastPlayed = getBlocking(PreferencesKeys.LAST_PLAYED)
    return stations.find { it.id == lastPlayed } ?: stations[0]
}

fun DataStore<Preferences>.isAutoPlayEnabled(): Boolean {
    return getBlocking(PreferencesKeys.AUTO_PLAY)
}

fun DataStore<Preferences>.getBottomNavigationUiPref(): BottomNavPreference {
    return BottomNavPreference.of(getBlocking(PreferencesKeys.BTM_NAV))
}

suspend fun <T> DataStore<Preferences>.get(key: PreferenceKey<T>): T {
    return get(key.key, key.defaultValue)
}

suspend fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, defValue: T): T {
    return data.map { it[key] ?: defValue }.first()
}

suspend fun <T> DataStore<Preferences>.get(block: (preferences: Preferences) -> T?): T? {
    return data.map {
        block(it)
    }.firstOrNull()
}

fun <T> DataStore<Preferences>.getBlocking(key: PreferenceKey<T>): T {
    return getBlocking(key.key, key.defaultValue)
}

fun <T> DataStore<Preferences>.getBlocking(key: Preferences.Key<T>, defValue: T): T {
    return runBlocking {
        data.map { it[key] ?: defValue }
            .catch { e ->
                Timber.e(e, "Error accessing Preferences datastore")
                emit(defValue)
            }
            .first()
    }
}

fun <T> DataStore<Preferences>.getBlocking(block: (preferences: Preferences) -> T?): T? {
    return runBlocking {
        get(block)
    }
}

suspend fun DataStore<Preferences>.contains(key: Preferences.Key<*>): Boolean {
    return data.map { it.contains(key) }.firstOrNull() == true
}

suspend fun <T> DataStore<Preferences>.update(key: PreferenceKey<T>, value: T) {
    edit { it[key.key] = value }
}

suspend fun DataStore<Preferences>.update(block: (preferences: MutablePreferences) -> Unit) {
    edit { block(it) }
}

fun <T> DataStore<Preferences>.updateBlocking(key: PreferenceKey<T>, value: T) {
    runCatching {
        runBlocking {
            edit {
                it[key.key] = value
            }
        }
    }
}

fun DataStore<Preferences>.updateBlocking(block: (preferences: MutablePreferences) -> Unit) {
    runCatching {
        runBlocking {
            edit { block(it) }
        }
    }
}

suspend fun DataStore<Preferences>.remove(vararg keys: PreferenceKey<*>) {
    edit {
        for (key in keys) {
            it.remove(key.key)
        }
    }
}

fun DataStore<Preferences>.removeBlocking(vararg keys: PreferenceKey<*>) {
    runCatching {
        runBlocking {
            remove(*keys)
        }
    }
}

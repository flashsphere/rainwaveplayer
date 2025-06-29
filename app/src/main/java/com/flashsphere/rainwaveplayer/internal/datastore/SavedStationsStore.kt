package com.flashsphere.rainwaveplayer.internal.datastore

import android.content.Context
import com.flashsphere.rainwaveplayer.internal.datastore.model.SavedStations
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SavedStationsStore @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val json: Json,
) {
    private val file = context.cacheDir.resolve("stations.json")

    suspend fun exists(): Boolean = withContext(coroutineDispatchers.network) {
        runCatching { file.exists() }.getOrElse { false }
    }

    suspend fun save(stations: List<Station>) = withContext(coroutineDispatchers.network) {
        val savedStations = SavedStations(
            timestamp = System.currentTimeMillis(),
            stations = stations.toList()
        )

        runCatching {
            file.bufferedWriter().use { it.write(json.encodeToString(savedStations)) }
            Timber.d("Updated stations local cache")
        }.onFailure {
            Timber.e(it, "Unable to store stations json")
            deleteFile()
        }.isSuccess
    }

    suspend fun get(): SavedStations? = withContext(coroutineDispatchers.network) {
        val savedStations = runCatching {
            if (!file.exists()) {
                return@withContext null
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            json.decodeFromString<SavedStations>(jsonString)
        }.getOrElse {
            Timber.e(it, "Unable to read stations json")
            deleteFile()
            return@withContext null
        }

        val timeElapsedDays = (System.currentTimeMillis() - savedStations.timestamp)
            .milliseconds.inWholeDays
        if (timeElapsedDays >= 7) {
            Timber.d("Not reusing stored stations list since %d days ago", timeElapsedDays)
            deleteFile()
            return@withContext null
        }

        return@withContext savedStations
    }

    private suspend fun deleteFile() = withContext(coroutineDispatchers.network) {
        runCatching {
            if (file.exists()) {
                Timber.d("Deleting stations json")
                file.delete()
            }
        }.onFailure {
            Timber.w(it, "Unable to delete stations json")
        }
    }

    suspend fun remove() {
        deleteFile()
    }
}

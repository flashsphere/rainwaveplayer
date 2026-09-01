package com.flashsphere.rainwaveplayer.util

import android.content.Context
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Singleton
class BlockStoreManager @Inject constructor(
    context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
) {
    private val client by lazy { Blockstore.getClient(context) }

    fun store(userCredentials: UserCredentials) {
        val credentials = "${userCredentials.userId}:${userCredentials.apiKey}"
        coroutineDispatchers.scope.launchWithDefaults("Store credentials in block storage", coroutineDispatchers.io) {
            suspendRunCatching {
                val end2EndEncryptionAvailable = client.isEndToEndEncryptionAvailable().await()

                Timber.d(
                    "Block store end to end encryption available: %s",
                    end2EndEncryptionAvailable
                )
                client.storeBytes(
                    StoreBytesData.Builder()
                        .setKey(CREDENTIALS_KEY)
                        .setBytes(credentials.toByteArray())
                        .setShouldBackupToCloud(end2EndEncryptionAvailable)
                        .build()
                ).await()
            }
            .onFailure { Timber.e(it, "Failed to store in block store") }
            .onSuccess { Timber.d("Stored in block store: %d bytes stored", it) }
        }
    }

    suspend fun retrieve(): UserCredentials? {
        val result = suspendRunCatching {
            client.retrieveBytes(
                RetrieveBytesRequest.Builder()
                    .setKeys(listOf(CREDENTIALS_KEY))
                    .build()
            ).await()
        }
        .onFailure { Timber.e(it, "Failed to retrieve from block store") }
        .getOrNull() ?: return null

        val data = result.blockstoreDataMap

        val credentials = data[CREDENTIALS_KEY]
            ?.bytes
            ?.toString(Charsets.UTF_8)

        if (credentials.isNullOrBlank()) return null

        val userInfo = credentials.split(":").toTypedArray()
        if (userInfo.size != 2) return null

        val userId = userInfo[0].toIntOrNull()
        val apiKey = userInfo[1]

        if (userId == null || userId == PreferencesKeys.USER_ID.defaultValue || apiKey.isBlank()) {
            return null
        }

        return UserCredentials(
            userId = userId,
            apiKey = apiKey
        )
    }

    suspend fun clear() {
        suspendRunCatching {
            client.deleteBytes(
                DeleteBytesRequest.Builder()
                    .setKeys(listOf(CREDENTIALS_KEY))
                    .build()
            ).await()
        }
        .onFailure { Timber.e(it, "Failed to clear from block store") }
        .onSuccess { Timber.d("Cleared from block store") }
    }

    companion object {
        private const val CREDENTIALS_KEY = "credentials"
    }
}

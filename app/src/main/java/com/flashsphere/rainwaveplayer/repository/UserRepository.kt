package com.flashsphere.rainwaveplayer.repository

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.API_KEY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.USER_ID
import com.flashsphere.rainwaveplayer.util.Strings.toEmpty
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.removeBlocking
import com.flashsphere.rainwaveplayer.util.updateBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    fun login(userCredentials: UserCredentials) {
        storeCredentials(userCredentials.userId, userCredentials.apiKey)
    }

    fun logout() {
        clearCredentials()
    }

    fun isLoggedIn() = getCredentials() != null

    fun getCredentials(): UserCredentials? {
        return dataStore.getBlocking { preferences ->
            val userId = preferences[USER_ID.key] ?: USER_ID.defaultValue
            val apiKey = preferences[API_KEY.key] ?: API_KEY.defaultValue
            if (userId != -1 && apiKey.isNotBlank()) {
                UserCredentials(userId, apiKey)
            } else {
                null
            }
        }
    }

    fun parseCredentialsUri(uri: Uri): UserCredentials? {
        val userInfo = uri.userInfo.toEmpty().split(":").toTypedArray()
        if (userInfo.size != 2) {
            return null
        }

        val userId = userInfo[0].toIntOrNull()
        val apiKey = userInfo[1]

        if (userId == null || apiKey.isBlank()) {
            return null
        }

        return UserCredentials(userId, apiKey)
    }

    private fun storeCredentials(userId: Int, key: String) {
        dataStore.updateBlocking { preferences ->
            preferences[USER_ID.key] = userId
            preferences[API_KEY.key] = key
        }
    }

    private fun clearCredentials() {
        dataStore.removeBlocking(USER_ID, API_KEY)
    }
}

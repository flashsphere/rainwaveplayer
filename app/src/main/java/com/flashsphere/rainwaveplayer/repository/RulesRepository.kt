package com.flashsphere.rainwaveplayer.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.autovote.v1.Rule
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_VOTE_RULES
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesRepository @Inject constructor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private var rules: List<Rule>? = null

    suspend fun get(): List<Rule> {
        return this.rules ?: withContext(coroutineDispatchers.io) {
            val rulesJson = dataStore.get(AUTO_VOTE_RULES)
            Timber.d("rulesJson = %s", rulesJson)
            if (rulesJson.isNotEmpty()) {
                runCatching { json.decodeFromString<List<Rule>>(rulesJson) }
                    .getOrElse { emptyList() }
            } else {
                emptyList()
            }
        }.also { this.rules = it }
    }

    suspend fun save(rules: List<Rule>) {
        withContext(coroutineDispatchers.io) {
            val rulesJson = runCatching { json.encodeToString(rules) }
                .getOrElse { "" }
            Timber.d("rulesJson = %s", rulesJson)
            dataStore.update(AUTO_VOTE_RULES, rulesJson)
        }
        this.rules = rules
    }
}

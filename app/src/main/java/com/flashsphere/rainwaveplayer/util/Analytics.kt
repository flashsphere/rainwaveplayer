package com.flashsphere.rainwaveplayer.util

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.model.station.Station
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(
    coroutineDispatchers: CoroutineDispatchers,
    dataStore: DataStore<Preferences>,
) {
    fun logEvent(eventName: String, station: Station?) {
    }

    @JvmOverloads
    fun logEvent(eventName: String, bundle: Bundle? = null) {
    }

    companion object {
        const val EVENT_LOGIN = "login"
        const val EVENT_PLAY_REQUEST = "play_request"
        const val EVENT_ADD_TILE = "add_tile"
        const val EVENT_REMOVE_TILE = "remove_tile"
        const val EVENT_USE_TILE = "use_tile"
        const val EVENT_USE_SHORTCUT = "use_shortcut"
        const val EVENT_VOTE_SONG_NOTIFICATION = "vote_song_notification"
        const val EVENT_OPEN_DISCORD = "open_discord"
        const val EVENT_OPEN_PATREON = "open_patreon"
        const val EVENT_SET_SLEEP_TIMER = "set_sleep_timer"
        const val EVENT_REMOVE_SLEEP_TIMER = "remove_sleep_timer"
        const val EVENT_SCREEN_VIEW = "screen_view"
        const val ITEM_ID = "item_id"
        const val ITEM_NAME = "item_name"
        const val SCREEN_NAME = "screen_name"
        const val SCREEN_CLASS = "screen_class"
    }
}

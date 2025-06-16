package com.flashsphere.rainwaveplayer.view.uistate.model

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.event.Event
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.util.getTimeRemaining
import kotlin.time.Duration

@Immutable
class StationInfo(
    val currentEventId: Int,
    val previouslyPlayed: PreviouslyPlayed,
    val nowPlaying: NowPlaying,
    val comingUp: List<ComingUp>,
) {
    val items = mutableStateListOf<StationInfoItem>().apply {
        add(previouslyPlayed.header)
        addAll(previouslyPlayed.items)
        add(nowPlaying.header)
        add(nowPlaying.item)
        comingUp.forEach {
            add(it.header)
            addAll(it.items)
        }
    }

    constructor(infoResponse: InfoResponse, includePreviouslyPlayedSongs: Boolean, isTv: Boolean) : this(
        currentEventId = infoResponse.currentEvent.id,
        previouslyPlayed = toPreviouslyPlayed(infoResponse, includePreviouslyPlayedSongs, isTv),
        nowPlaying = toNowPlaying(infoResponse),
        comingUp = toComingUp(infoResponse),
    )

    companion object {
        private fun toPreviouslyPlayed(
            infoResponse: InfoResponse,
            includePreviouslyPlayedSongs: Boolean,
            isTv: Boolean,
        ): PreviouslyPlayed {
            val items = if (!isTv) {
                infoResponse.previousEvents
            } else {
                infoResponse.previousEvents.reversed()
            }
            return PreviouslyPlayed(
                header = PreviouslyPlayedHeaderItem(),
                items = if (includePreviouslyPlayedSongs || isTv) {
                    items.map {
                        val song = it.songs.first()
                        PreviouslyPlayedSongItem(it.id, StationInfoSongData(song))
                    }
                } else {
                    emptyList()
                }
            )
        }
        private fun toNowPlaying(infoResponse: InfoResponse): NowPlaying {
            return NowPlaying(
                header = NowPlayingHeaderItem(infoResponse.currentEvent,
                    infoResponse.apiInfo.timeDifference),
                item = NowPlayingSongItem(infoResponse.currentEvent, infoResponse.apiInfo.timeDifference)
            )
        }
        private fun toComingUp(infoResponse: InfoResponse): List<ComingUp> {
            return infoResponse.futureEvents.map { event ->
                ComingUp(
                    header = ComingUpHeaderItem(event),
                    items = event.songs.map {
                        ComingUpSongItem(event, it)
                    }
                )
            }
        }
    }
}

interface StationInfoItem {
    val contentType: String
    val key: String
}

interface StationInfoHeaderItem : StationInfoItem
interface StationInfoSongItem : StationInfoItem {
    val eventId: Int
    val data: StationInfoSongData
}

@Immutable
class PreviouslyPlayed(
    val header: PreviouslyPlayedHeaderItem,
    val items: List<PreviouslyPlayedSongItem>,
)

@Immutable
class PreviouslyPlayedHeaderItem: StationInfoHeaderItem {
    override val contentType: String = "previously-played-header"
    override val key: String = "previously-played-header"
}

@Immutable
class PreviouslyPlayedSongItem(
    override val eventId: Int,
    override val data: StationInfoSongData,
) : StationInfoSongItem {
    override val contentType: String = "song"
    override val key: String = "song-${data.song.id}"
}

@Immutable
class NowPlaying(
    val header: NowPlayingHeaderItem,
    val item: NowPlayingSongItem,
)

@Immutable
class NowPlayingHeaderItem(
    val eventId: Int,
    val eventName: String,
    val eventEndTime: Long,
    val apiTimeDifference: Duration,
) : StationInfoHeaderItem {
    override val contentType: String = "now-playing-header"
    override val key: String = "event-${eventId}"

    constructor(event: Event, apiTimeDifference: Duration) : this(
        eventId = event.id,
        eventName = event.name,
        eventEndTime = event.endTime,
        apiTimeDifference = apiTimeDifference,
    )
}

@Immutable
class NowPlayingSongItem(
    override val eventId: Int,
    override val data: StationInfoSongData,
    val eventEndTime: Long,
    val apiTimeDifference: Duration,
) : StationInfoSongItem {
    override val contentType: String = "song"
    override val key: String = "song-${data.song.id}"

    constructor(event: Event, apiTimeDifference: Duration) : this(
        eventId = event.id,
        data = StationInfoSongData(event.songs.first()),
        eventEndTime = event.endTime,
        apiTimeDifference = apiTimeDifference,
    )

    fun getCurrentPosition(): Long {
        return data.song.length - getTimeRemaining()
    }
}

@Immutable
class ComingUp(
    val header: ComingUpHeaderItem,
    val items: List<ComingUpSongItem>,
)

@Immutable
class ComingUpHeaderItem(
    val eventId: Int,
    val eventName: String,
    val votingAllowed: Boolean,
) : StationInfoHeaderItem {
    override val contentType: String = "coming-up-header"
    override val key: String = "coming-up-${eventId}"

    constructor(event: Event) : this(
        eventId = event.id,
        eventName = event.name,
        votingAllowed = event.votingAllowed,
    )

    fun getEventName(context: Context, isLoggedIn: Boolean): String {
        return if (votingAllowed && isLoggedIn) {
            if (eventName.isNotEmpty()) {
                context.getString(R.string.vote_now_event, eventName)
            } else {
                context.getString(R.string.coming_up_event, context.getString(R.string.vote_now))
            }
        } else {
            if (eventName.isNotEmpty()) {
                context.getString(R.string.coming_up_event, eventName)
            } else {
                context.getString(R.string.coming_up)
            }
        }
    }
}

@Immutable
class ComingUpSongItem(
    override val eventId: Int,
    override val data: StationInfoSongData,
) : StationInfoSongItem {
    override val contentType: String = "song"
    override val key: String = "song-${data.song.id}"

    constructor(event: Event, song: Song) : this(
        eventId = event.id,
        data = StationInfoSongData(song),
    )
}

@Immutable
class StationInfoSongData(
    val song: SongState,
    val album: AlbumState,
    val requestorId: Int,
    val requestorName: String,
) {
    val songId = song.id
    val albumId = album.id

    constructor(song: Song) : this(
        song = SongState(song),
        album = AlbumState(song.albums.first()),
        requestorId = song.userIdRequested,
        requestorName = song.usernameRequested,
    )
}

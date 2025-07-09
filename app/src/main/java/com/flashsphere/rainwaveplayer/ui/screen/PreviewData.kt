package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistAlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistDetailItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryDetailItem
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUp
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlaying
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreviouslyPlayed
import com.flashsphere.rainwaveplayer.view.uistate.model.PreviouslyPlayedHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreviouslyPlayedSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfo
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongData
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val stations = listOf(
    Station(id = 1, name = "Game", stream = "", description = "", relays = listOf()),
    Station(id = 2, name = "OC Remix", stream = "", description = "", relays = listOf()),
    Station(id = 3, name = "Chiptunes", stream = "", description = "", relays = listOf()),
    Station(id = 4, name = "Covers", stream = "", description = "", relays = listOf()),
    Station(id = 5, name = "All", stream = "", description = "", relays = listOf()),
)

val songStateData = listOf(SongState(
    id = 24885,
    title = "Athletic Theme (Super Mario 3D World)",
    albumName = "Music System 2013",
    artistName = "Zebes System",
    rating = 3.9F,
    ratingUser = mutableFloatStateOf(0F),
    favorite = mutableStateOf(true),
    cool = false,
    requestable = true,
    entryId = 1111,
    voted = mutableStateOf(false),
    votingAllowed = true,
    ratingAllowed = true,
    length = 123,
    albums = listOf(AlbumState(2860, "Music System 2013")),
    artists = listOf(ArtistState(1470, "Zebes System")),
), SongState(
    id = 4738,
    title = "Zelda: Link's Awakening - Tal Tal Heights",
    albumName = "Super Smash Bros. Brawl",
    artistName = "Yusuke Takahama",
    rating = 3.9F,
    ratingUser = mutableFloatStateOf(1.5F),
    favorite = mutableStateOf(false),
    cool = true,
    requestable = true,
    entryId = 2222,
    voted = mutableStateOf(true),
    votingAllowed = true,
    ratingAllowed = true,
    length = 123,
    albums = listOf(AlbumState(262, "Super Smash Bros. Brawl")),
    artists = listOf(ArtistState(10550, "Yusuke Takahama")),
), SongState(
    id = 6213,
    title = "To the Other Side of the Sea",
    albumName = "Twinbee Yahho!",
    artistName = "Akihiro Juichiya, Kazuhiro Senoo, Naoki Maeda",
    rating = 1.0F,
    ratingUser = mutableFloatStateOf(2.5F),
    favorite = mutableStateOf(true),
    cool = false,
    requestable = true,
    entryId = 3333,
    voted = mutableStateOf(false),
    votingAllowed = false,
    ratingAllowed = false,
    length = 123,
    albums = listOf(AlbumState(643, "Twinbee Yahho!")),
    artists = listOf(
        ArtistState(16516, "Akihiro Juichiya"),
        ArtistState(16621, "Kazuhiro Senoo"),
        ArtistState(9428, "Naoki Maeda")
    ),
), SongState(
    id = 28756,
    title = "Song of the Ancients / Devola",
    albumName = "NieR Music Concert & Talk Live Soundtrack",
    artistName = "MONACA",
    rating = 3.9F,
    ratingUser = mutableFloatStateOf(2.5F),
    favorite = mutableStateOf(true),
    cool = false,
    requestable = true,
    entryId = 4444,
    voted = mutableStateOf(true),
    votingAllowed = true,
    ratingAllowed = false,
    length = 123,
    albums = listOf(AlbumState(3300, "NieR Music Concert & Talk Live Soundtrack")),
    artists = listOf(ArtistState(22572, "MONACA")),
), SongState(
    id = 35738,
    title = "Airship",
    albumName = "Penny Arcade Adventures: On The Rain-Slick Precipice Of Darkness: Episode 4",
    artistName = "HyperDuck SoundWorks",
    rating = 3.9F,
    ratingUser = mutableFloatStateOf(0F),
    favorite = mutableStateOf(true),
    cool = false,
    requestable = true,
    entryId = 4444,
    voted = mutableStateOf(false),
    votingAllowed = true,
    ratingAllowed = false,
    length = 123,
    albums = listOf(AlbumState(3986, "Penny Arcade Adventures: On The Rain-Slick Precipice Of Darkness: Episode 4")),
    artists = listOf(ArtistState(9262, "HyperDuck SoundWorks")),
))

val albumStateData = listOf(
    AlbumState(
        id = 3763,
        name = ".hack//G.U.",
        art = "/album_art/1_3763",
        rating = 3.9F,
        ratingUser = 3.9F,
        cool = true,
        ratingCount = 533,
        favorite = mutableStateOf(false),
        ratingDistribution = mapOf(1 to 0.5F, 2 to 0.4F, 3 to 0.3F, 4 to 0.2F, 5 to 0.1F),
        songsOnCooldown = false,
        songs = mutableStateListOf(songStateData[1], songStateData[2]),
    ), AlbumState(
        id = 3764,
        name = ".hack//Infection",
        art = "/album_art/1_3764",
        rating = 3.9F,
        ratingUser = 0F,
        cool = false,
        ratingCount = 533,
        favorite = mutableStateOf(true),
        ratingDistribution = mapOf(1 to 0.1F, 2 to 0.3F, 3 to 0.6F, 4 to 0.4F, 5 to 0.1F),
        songsOnCooldown = false,
        songs = mutableStateListOf(songStateData[3], songStateData[4]),
    )
)

val artistStateData = listOf(
    ArtistState(
        id = 1,
        name = "Artist name Artist name Artist name Artist name Artist name Artist name",
        items = mutableStateListOf<ArtistDetailItem>().apply {
            add(
                ArtistAlbumState(
                stationId = 1, stationName = "Game",
                albumId = 1, albumName = songStateData[0].albumName,
                showStationName = false)
            )
            addAll(mutableListOf(
                songStateData[0], songStateData[1]))
            add(
                ArtistAlbumState(
                stationId = 2, stationName = "OC Remix",
                albumId = 2, albumName = songStateData[2].albumName,
                showStationName = true)
            )
            addAll(mutableListOf(songStateData[2]))
        }
    )
)

val categoryStateData = listOf(
    CategoryState(
        id = 1,
        name = "Some category",
        items = mutableStateListOf<CategoryDetailItem>().apply {
            add(AlbumState(id = 1, name = songStateData[0].albumName))
            addAll(mutableListOf(
                songStateData[0], songStateData[1]))
            add(AlbumState(id = 2, name = songStateData[2].albumName))
            addAll(mutableListOf(songStateData[2]))
        }
    )
)

val userStateData = listOf(
    UserState(
        id = 1, name = "Test User", avatar = "", requestsPaused = false, requestPosition = 1
    ),
    UserState(
        id = 2, name = "Test User", avatar = "", requestsPaused = true, requestPosition = 1
    ),
)

val requestStateData = listOf(
    RequestState(
        id = 1,
        title = "Border of Life",
        art = "abc.jpg",
        songId = 1,
        albumName = "Touhou Youyoumu: Perfect Cherry Blossom",
        valid = true,
        good = true,
        cooldown = false,
        cooldownEndTime = 0,
        electionBlocked = false,
        electionBlockedBy = "group",
        stationName = "Game",
    ),
    RequestState(
        id = 2,
        title = "Made in U.S.A.",
        art = "abc.jpg",
        songId = 2,
        albumName = "Super Street Fighter II Turbo HD",
        valid = false,
        good = true,
        cooldown = false,
        cooldownEndTime = 0,
        electionBlocked = true,
        electionBlockedBy = "group",
        stationName = "Game",
    ),
    RequestState(
        id = 3,
        title = "Thine Wrath...",
        art = "abc.jpg",
        songId = 3,
        albumName = "The Binding of Isaac",
        valid = false,
        good = true,
        cooldown = true,
        cooldownEndTime = 1722776461,
        electionBlocked = false,
        electionBlockedBy = "group",
        stationName = "Game",
    ),
    RequestState(
        id = 4,
        title = "Unholy Crystal Boss",
        art = "abc.jpg",
        songId = 4,
        albumName = "Splatterhouse 2",
        valid = false,
        good = true,
        cooldown = false,
        cooldownEndTime = 1722776461,
        electionBlocked = true,
        electionBlockedBy = "album",
        stationName = "Game",
    ),
    RequestState(
        id = 5,
        title = "Desert Dungeon",
        art = "abc.jpg",
        songId = 5,
        albumName = "Rune Factory 3",
        valid = false,
        good = false,
        cooldown = false,
        cooldownEndTime = 1722776461,
        electionBlocked = false,
        electionBlockedBy = "",
        stationName = "Game",
    ),
)

val stationInfoData = StationInfo(
    currentEventId = 1,
    previouslyPlayed = PreviouslyPlayed(
        header = PreviouslyPlayedHeaderItem(),
        items = listOf(
            PreviouslyPlayedSongItem(
                eventId = 1,
                StationInfoSongData(
                    song = songStateData[0],
                    album = AlbumState(
                        id = 1,
                        name = songStateData[0].albumName
                    ).apply { favorite.value = true },
                    requestorId = 2,
                    requestorName = "Test User"
                ),
            ),
            PreviouslyPlayedSongItem(
                eventId = 2,
                StationInfoSongData(
                    song = songStateData[1],
                    album = AlbumState(
                        id = 1,
                        name = songStateData[1].albumName
                    ),
                    requestorId = 0,
                    requestorName = ""
                ),
            ),
        )
    ),
    nowPlaying = NowPlaying(
        header = NowPlayingHeaderItem(
            eventId = 3,
            eventName = "",
            eventEndTime = System.currentTimeMillis().milliseconds.inWholeSeconds + 100,
            apiTimeDifference = 0.seconds,
        ),
        item = NowPlayingSongItem(
            eventId = 3,
            StationInfoSongData(
                song = songStateData[2],
                album = AlbumState(
                    id = 1,
                    name = songStateData[2].albumName
                ),
                requestorId = 3,
                requestorName = "Test User"
            ),
            eventEndTime = 12345,
            apiTimeDifference = 0.seconds,
        ),
    ),
    comingUp = listOf(
        ComingUp(
            header = ComingUpHeaderItem(
                eventId = 4,
                eventName = "Test",
                votingAllowed = true
            ),
            items = listOf(
                ComingUpSongItem(
                    eventId = 4,
                    StationInfoSongData(
                        song = songStateData[3],
                        album = AlbumState(
                            id = 1,
                            name = songStateData[3].albumName
                        ).apply {
                            favorite.value = true
                        },
                        requestorId = 1,
                        requestorName = "User Name User Name"
                    ),
                ),
                ComingUpSongItem(
                    eventId = 4,
                    StationInfoSongData(
                        song = songStateData[4],
                        album = AlbumState(
                            id = 1,
                            name = songStateData[4].albumName
                        ).apply {
                            favorite.value = true
                        },
                        requestorId = 0,
                        requestorName = ""
                    ),
                )
            ),
        ),
        ComingUp(
            header = ComingUpHeaderItem(eventId = 5, eventName = "", votingAllowed = false),
            items = listOf(),
        ),
    ),
)

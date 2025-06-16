package com.flashsphere.rainwaveplayer.autovote.v1

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition.Operator
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.event.Event
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.util.buildJson
import com.flashsphere.rainwaveplayer.util.readFile
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test

class RuleTest {
    @Test
    fun fave_song_and_greater_equal_rating_rule_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
                favoriteSong = true,
                rating = 4F,
            ), createSong(
                id = 2,
                favoriteSong = true,
                rating = 4F,
                ratingUser = 4.2F,
            ), createSong(
                id = 3,
                favoriteSong = true,
                rating = 4F,
                ratingUser = 4.8F,
            ))
        )
        val rule = Rule(1, listOf(
            FaveSongCondition,
            RatingCondition(Operator.GreaterEqual, 4F)
        ))
        val song = rule.apply(event, RuleParams(2))
        assertThat(song?.id).isEqualTo(event.songs[2].id)
    }

    @Test
    fun fave_album_and_greater_equal_rating_rule_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
                favoriteSong = true,
                rating = 4F,
            ), createSong(
                id = 2,
                favoriteAlbum = true,
                rating = 4.2F,
            ), createSong(
                id = 3,
                favoriteAlbum = true,
                rating = 4F,
                ratingUser = 3.8F,
            ))
        )
        val rule = Rule(1, listOf(
            FaveAlbumCondition,
            RatingCondition(Operator.GreaterEqual, 4F)
        ))
        val song = rule.apply(event, RuleParams(2))
        assertThat(song?.id).isEqualTo(event.songs[1].id)
    }

    @Test
    fun fave_song_and_lesser_equal_rating_rule_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
                favoriteSong = true,
                rating = 3F,
            ), createSong(
                id = 2,
                favoriteSong = true,
                rating = 4F,
                ratingUser = 1F,
            ), createSong(
                id = 3,
                favoriteSong = false,
                rating = 4F,
                ratingUser = 1F,
            ))
        )
        val rule = Rule(1, listOf(
            FaveSongCondition,
            RatingCondition(Operator.LesserEqual, 4F)
        ))
        val song = rule.apply(event, RuleParams(2))
        assertThat(song?.id).isEqualTo(event.songs[1].id)
    }

    @Test
    fun fave_album_and_lesser_equal_rating_rule_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
                favoriteSong = true,
                rating = 1F,
            ), createSong(
                id = 2,
                favoriteAlbum = true,
                rating = 2.2F,
            ), createSong(
                id = 3,
                favoriteAlbum = true,
                rating = 4F,
                ratingUser = 1F,
            ))
        )
        val rule = Rule(1, listOf(
            FaveAlbumCondition,
            RatingCondition(Operator.LesserEqual, 4F)
        ))
        val song = rule.apply(event, RuleParams(2))
        assertThat(song?.id).isEqualTo(event.songs[2].id)
    }

    @Test
    fun user_request_rule_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
            ), createSong(
                id = 2,
                userIdRequested = 2,
            ), createSong(
                id = 3,
            ))
        )
        val rule = Rule(1, listOf(
            RequestCondition(RequestCondition.RequestType.User),
        ))
        val song = rule.apply(event, RuleParams(2))
        assertThat(song?.id).isEqualTo(event.songs[1].id)
    }

    @Test
    fun others_request_and_fave_song_rules_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
                favoriteSong = true,
                userIdRequested = 2,
            ), createSong(
                id = 2,
                favoriteSong = true,
                userIdRequested = 1,
            ), createSong(
                id = 3,
                favoriteSong = true,
            ))
        )
        val rule = Rule(1, listOf(
            RequestCondition(RequestCondition.RequestType.Others),
            FaveSongCondition,
        ))
        val song = rule.apply(event, RuleParams(2))
        assertThat(song?.id).isEqualTo(event.songs[1].id)
    }

    @Test
    fun multiple_rules_test() {
        val event = createEvent(
            songs = listOf(createSong(
                id = 1,
                favoriteSong = true,
                favoriteAlbum = true,
                userIdRequested = 3,
                ratingUser = 5F,
                rating = 4.8F,
            ), createSong(
                id = 2,
                favoriteSong = true,
                rating = 4.3F,
            ), createSong(
                id = 3,
                rating = 4.3F,
            ))
        )
        val rules = listOf(
            Rule(1, listOf(RequestCondition(RequestCondition.RequestType.User))),
            Rule(2, listOf(FaveSongCondition)),
            Rule(3, listOf(FaveAlbumCondition, RatingCondition(Operator.GreaterEqual, 4F))),
            Rule(4, listOf(RatingCondition(Operator.GreaterEqual, 4F)))
        )

        val ruleParams = RuleParams(2)
        val song = rules.asSequence().map { it.apply(event, ruleParams) }
            .filterNotNull()
            .firstOrNull()
        assertThat(song?.id).isEqualTo(event.songs[0].id)
    }

    private fun createEvent(
        songs: List<Song>
    ): Event {
        return Event(
            id = 1,
            name = "",
            type = "",
            stationId = 1,
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            votingAllowed = true,
            songs = songs,
        )
    }

    private fun createSong(
        id: Int,
        userIdRequested: Int = 0,
        favoriteSong: Boolean = false,
        favoriteAlbum: Boolean = false,
        ratingUser: Float = 0F,
        rating: Float = 0F,
    ): Song {
        return Song(
            id = id,
            title = "Song $id",
            rating = rating,
            ratingUser = ratingUser,
            artists = listOf(),
            albums = listOf(Album(name = "Album for Song $id", favorite = favoriteAlbum)),
            ratingAllowed = false,
            entryId = 1,
            cool = false,
            requestable = false,
            favorite = favoriteSong,
            userIdRequested = userIdRequested,
            usernameRequested = "",
            stationId = 1,
            voted = false,
            votingAllowed = true,
            length = 0,
        )
    }

    @Test
    fun serialize_to_json() {
        val json = buildJson()
        val expectedResultJson = readFile(this.javaClass, "/json/auto-vote-v1-rule.json")

        val rule = Rule(1, listOf(
            RequestCondition(RequestCondition.RequestType.Others),
            RatingCondition(Operator.Greater, 4.2F),
            FaveSongCondition,
            FaveAlbumCondition,
        ))
        val jsonString = json.encodeToString(rule)
        JSONAssert.assertEquals(expectedResultJson, jsonString, true)
    }

    @Test
    fun deserialize_from_json() {
        val json = buildJson()
        val expectedRule = Rule(1, listOf(
            RequestCondition(RequestCondition.RequestType.Others),
            RatingCondition(Operator.Greater, 4.2F),
            FaveSongCondition,
            FaveAlbumCondition,
        ))

        val rule = json.decodeFromString<Rule>(readFile(this.javaClass, "/json/auto-vote-v1-rule.json"))
        assertThat(rule).isEqualTo(expectedRule)
    }
}

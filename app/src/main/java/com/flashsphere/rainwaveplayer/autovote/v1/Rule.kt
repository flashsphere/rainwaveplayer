package com.flashsphere.rainwaveplayer.autovote.v1

import android.os.Parcelable
import androidx.annotation.StringRes
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.autovote.v1.Condition.FaveCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition.Operator
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition.RequestType
import com.flashsphere.rainwaveplayer.autovote.v1.Rule.ConditionType
import com.flashsphere.rainwaveplayer.model.event.Event
import com.flashsphere.rainwaveplayer.model.song.Song
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Parcelize
@Serializable
data class Rule(
    val id: Long,
    val conditions: List<Condition>
): Parcelable {
    fun apply(event: Event, params: RuleParams): Song? {
        if (conditions.isEmpty()) return null

        return conditions.asSequence()
            .map { it.apply(event.songs, params) }
            .reduceOrNull { acc, map ->
                if (acc.isEmpty() || map.isEmpty()) {
                    emptyMap()
                } else {
                    HashMap<Song, Int>().also { resultMap ->
                        acc.forEach { (k, v) ->
                            map[k]?.let { weight ->
                                resultMap[k] = v + weight
                            }
                        }
                    }
                }
            }
            ?.maxByOrNull { (_, v) -> v }
            ?.key
    }

    enum class ConditionType(
        @StringRes
        val stringResId: Int
    ) {
        Request(R.string.auto_vote_condition_request),
        Rating(R.string.auto_vote_condition_rating),
        FaveSong(R.string.auto_vote_condition_fave_song),
        FaveAlbum(R.string.auto_vote_condition_fave_album);
    }
}

data class RuleParams(
    val userId: Int,
)

@Serializable
sealed interface Condition : Parcelable {
    val conditionType: ConditionType
    fun apply(songs: List<Song>, params: RuleParams): Map<Song, Int>

    @Serializable
    sealed interface FaveCondition : Condition {
        fun getFave(song: Song): Boolean
        override fun apply(songs: List<Song>, params: RuleParams): Map<Song, Int> {
            return songs.asSequence()
                .filter { getFave(it) }
                .associateWith { 1 }
        }
    }
    companion object {
        fun new(conditionType: ConditionType): Condition {
            return when (conditionType) {
                ConditionType.Request -> RequestCondition(RequestType.User)
                ConditionType.Rating -> RatingCondition(Operator.GreaterEqual, 0F)
                ConditionType.FaveSong -> FaveSongCondition
                ConditionType.FaveAlbum -> FaveAlbumCondition
            }
        }
    }
}

@Parcelize
@Serializable
data class RequestCondition(
    val requestType: RequestType,
) : Condition {
    @IgnoredOnParcel
    override val conditionType: ConditionType = ConditionType.Request
    override fun apply(songs: List<Song>, params: RuleParams): Map<Song, Int> {
        return songs.asSequence()
            .filter { song ->
                when (requestType) {
                    RequestType.User -> song.userIdRequested == params.userId
                    RequestType.Others -> song.userIdRequested != 0 && song.userIdRequested != params.userId
                }
            }
            .associateWith { 1 }
    }
    enum class RequestType(
        @StringRes
        val shortStringResId: Int,
        @StringRes
        val fullStringResId: Int,
    ) {
        User(R.string.auto_vote_condition_request_yours_short, R.string.auto_vote_condition_request_yours),
        Others(R.string.auto_vote_condition_request_others_short, R.string.auto_vote_condition_request_others),
    }
}

@Parcelize
@Serializable
data class RatingCondition(
    val operator: Operator,
    val rating: Float
): Condition {
    @IgnoredOnParcel
    override val conditionType: ConditionType = ConditionType.Rating
    private fun getRating(song: Song): Float {
        return if (song.ratingUser > 0) {
            song.ratingUser
        } else {
            song.rating
        }
    }
    override fun apply(songs: List<Song>, params: RuleParams): Map<Song, Int> {
        val filteredSongs = songs.asSequence()
            .filter { song -> operator.apply(getRating(song), rating) }
            .map { song -> song to getRating(song) }
            .toMap()

        val weights = filteredSongs.values.asSequence()
            .distinct()
            .let {
                when (operator) {
                    Operator.GreaterEqual,
                    Operator.Greater -> {
                        it.sorted()
                    }
                    Operator.LesserEqual,
                    Operator.Lesser -> {
                        it.sortedDescending()
                    }
                }
            }
            .mapIndexed { index, rating -> rating to index + 1 }
            .toMap(HashMap())

        return filteredSongs.asSequence()
            .map { (k, v) -> k to weights.getOrDefault(v, 0) }
            .toMap()
    }
    enum class Operator(val value: String) {
        GreaterEqual(">="),
        Greater(">"),
        LesserEqual("<="),
        Lesser("<");

        fun apply(left: Float, right: Float): Boolean {
            return when (this) {
                GreaterEqual -> left >= right
                Greater -> left > right
                LesserEqual -> left <= right
                Lesser -> left < right
            }
        }
    }
}

@Parcelize
@Serializable
data object FaveSongCondition : FaveCondition {
    @IgnoredOnParcel
    override val conditionType: ConditionType = ConditionType.FaveSong
    override fun getFave(song: Song): Boolean = song.favorite
}

@Parcelize
@Serializable
data object FaveAlbumCondition : FaveCondition {
    @IgnoredOnParcel
    override val conditionType: ConditionType = ConditionType.FaveAlbum
    override fun getFave(song: Song): Boolean = song.albums.first().favorite
}

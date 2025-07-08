package com.flashsphere.rainwaveplayer.repository

import com.flashsphere.rainwaveplayer.model.album.AlbumResponse
import com.flashsphere.rainwaveplayer.model.album.AllAlbums
import com.flashsphere.rainwaveplayer.model.artist.AllArtists
import com.flashsphere.rainwaveplayer.model.artist.ArtistResponse
import com.flashsphere.rainwaveplayer.model.category.AllCategories
import com.flashsphere.rainwaveplayer.model.category.CategoryResponse
import com.flashsphere.rainwaveplayer.model.favorite.AllFavesResponse
import com.flashsphere.rainwaveplayer.model.favorite.FaveAlbumResponse
import com.flashsphere.rainwaveplayer.model.favorite.FaveSongResponse
import com.flashsphere.rainwaveplayer.model.request.ClearRequestsResponse
import com.flashsphere.rainwaveplayer.model.request.DeleteRequestResponse
import com.flashsphere.rainwaveplayer.model.request.OrderRequestsResponse
import com.flashsphere.rainwaveplayer.model.request.PauseRequestResponse
import com.flashsphere.rainwaveplayer.model.request.RequestFaveResponse
import com.flashsphere.rainwaveplayer.model.request.RequestHistoryResponse
import com.flashsphere.rainwaveplayer.model.request.RequestSongResponse
import com.flashsphere.rainwaveplayer.model.request.RequestUnratedResponse
import com.flashsphere.rainwaveplayer.model.request.ResumeRequestResponse
import com.flashsphere.rainwaveplayer.model.search.SearchResponse
import com.flashsphere.rainwaveplayer.model.song.RateSongResponse
import com.flashsphere.rainwaveplayer.model.station.StationsResponse
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.model.vote.RecentVotesResponse
import com.flashsphere.rainwaveplayer.model.vote.VoteResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RainwaveService {
    @POST("stations")
    suspend fun fetchStations(): StationsResponse

    @FormUrlEncoded
    @POST("info")
    suspend fun fetchStationInfo(@Field("sid") stationId: Int): InfoResponse

    @FormUrlEncoded
    @POST("vote")
    suspend fun vote(@Field("sid") stationId: Int,
             @Field("entry_id") entryId: Int): VoteResponse

    @FormUrlEncoded
    @POST("all_albums")
    suspend fun fetchAllAlbums(@Field("sid") stationId: Int): AllAlbums

    @FormUrlEncoded
    @POST("all_artists")
    suspend fun fetchAllArtists(@Field("sid") stationId: Int): AllArtists

    @FormUrlEncoded
    @POST("all_groups")
    suspend fun fetchAllGroups(@Field("sid") stationId: Int): AllCategories

    @FormUrlEncoded
    @POST("album")
    suspend fun fetchAlbum(@Field("sid") stationId: Int,
                           @Field("id") albumId: Int): AlbumResponse

    @FormUrlEncoded
    @POST("artist")
    suspend fun fetchArtist(@Field("sid") stationId: Int,
                            @Field("id") artistId: Int): ArtistResponse

    @FormUrlEncoded
    @POST("group")
    suspend fun fetchGroup(@Field("sid") stationId: Int,
                           @Field("id") groupId: Int): CategoryResponse

    @FormUrlEncoded
    @POST("fave_album")
    suspend fun favoriteAlbum(@Field("sid") stationId: Int,
                      @Field("album_id") albumId: Int,
                      @Field("fave") favorite: Boolean): FaveAlbumResponse

    @FormUrlEncoded
    @POST("fave_song")
    suspend fun favoriteSong(@Field("song_id") songId: Int,
                     @Field("fave") favorite: Boolean): FaveSongResponse

    @FormUrlEncoded
    @POST("request")
    suspend fun requestSong(@Field("sid") stationId: Int,
                    @Field("song_id") songId: Int): RequestSongResponse

    @FormUrlEncoded
    @POST("order_requests")
    suspend fun orderRequests(@Field("sid") stationId: Int,
                      @Field("order") songIds: String): OrderRequestsResponse

    @FormUrlEncoded
    @POST("delete_request")
    suspend fun deleteRequest(@Field("sid") stationId: Int,
                              @Field("song_id") songId: Int): DeleteRequestResponse

    @FormUrlEncoded
    @POST("pause_request_queue")
    suspend fun pauseRequestQueue(@Field("sid") stationId: Int): PauseRequestResponse

    @FormUrlEncoded
    @POST("unpause_request_queue")
    suspend fun resumeRequestQueue(@Field("sid") stationId: Int): ResumeRequestResponse

    @FormUrlEncoded
    @POST("clear_requests")
    suspend fun clearRequests(@Field("sid") stationId: Int): ClearRequestsResponse

    @FormUrlEncoded
    @POST("request_favorited_songs")
    suspend fun requestFave(@Field("sid") stationId: Int): RequestFaveResponse

    @FormUrlEncoded
    @POST("request_unrated_songs")
    suspend fun requestUnrated(@Field("sid") stationId: Int): RequestUnratedResponse

    @FormUrlEncoded
    @POST("clear_rating")
    suspend fun clearRating(@Field("sid") stationId: Int,
                            @Field("song_id") songId: Int): RateSongResponse

    @FormUrlEncoded
    @POST("rate")
    suspend fun rateSong(@Field("sid") stationId: Int,
                         @Field("song_id") songId: Int,
                         @Field("rating") rating: Float): RateSongResponse

    @FormUrlEncoded
    @POST("search")
    suspend fun search(@Field("sid") stationId: Int,
                       @Field("search") query: String): SearchResponse

    @FormUrlEncoded
    @POST("all_faves")
    suspend fun allFaves(@Field("sid") stationId: Int,
                         @Field("per_page") perPage: Int,
                         @Field("page_start") pageStart: Int): AllFavesResponse

    @FormUrlEncoded
    @POST("user_requested_history")
    suspend fun requestHistory(@Field("sid") stationId: Int,
                               @Field("per_page") perPage: Int,
                               @Field("page_start") pageStart: Int): RequestHistoryResponse

    @FormUrlEncoded
    @POST("user_recent_votes")
    suspend fun recentVotes(@Field("sid") stationId: Int,
                            @Field("per_page") perPage: Int,
                            @Field("page_start") pageStart: Int): RecentVotesResponse

    companion object {
        const val BASE_URL = "https://rainwave.cc"
        const val API_URL = "$BASE_URL/api4/"
    }
}

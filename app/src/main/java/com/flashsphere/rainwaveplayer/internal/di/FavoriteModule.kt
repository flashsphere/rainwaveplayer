package com.flashsphere.rainwaveplayer.internal.di

import com.flashsphere.rainwaveplayer.model.favorite.FaveAlbumResponse
import com.flashsphere.rainwaveplayer.model.favorite.FaveSongResponse
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.view.viewmodel.FaveAlbumDelegate
import com.flashsphere.rainwaveplayer.view.viewmodel.FaveSongDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FavoriteModule {

    @Provides
    @Singleton
    fun provideFaveSongDelegate(stationRepository: StationRepository,
                                retrofit: Retrofit): FaveSongDelegate {
        val converter: Converter<ResponseBody, FaveSongResponse> =
            retrofit.responseBodyConverter(FaveSongResponse::class.java, emptyArray())
        return FaveSongDelegate(stationRepository, converter)
    }

    @Provides
    @Singleton
    fun provideFaveAlbumDelegate(stationRepository: StationRepository,
                                 retrofit: Retrofit): FaveAlbumDelegate {
        val converter: Converter<ResponseBody, FaveAlbumResponse> =
            retrofit.responseBodyConverter(FaveAlbumResponse::class.java, emptyArray())
        return FaveAlbumDelegate(stationRepository, converter)
    }
}

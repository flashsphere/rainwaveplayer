package com.flashsphere.rainwaveplayer.internal.di

import com.flashsphere.rainwaveplayer.model.song.RateSongResponse
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.view.viewmodel.RateSongDelegate
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
object RateSongModule {
    @Provides
    @Singleton
    fun provideRateSongDelegate(
        stationRepository: StationRepository,
        retrofit: Retrofit
    ): RateSongDelegate {
        val converter: Converter<ResponseBody, RateSongResponse> =
            retrofit.responseBodyConverter(RateSongResponse::class.java, emptyArray())
        return RateSongDelegate(stationRepository, converter)
    }
}

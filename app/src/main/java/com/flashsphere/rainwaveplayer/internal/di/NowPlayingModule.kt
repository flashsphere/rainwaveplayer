package com.flashsphere.rainwaveplayer.internal.di

import com.flashsphere.rainwaveplayer.model.station.StationsErrorResponse
import com.flashsphere.rainwaveplayer.model.vote.VoteResponse
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.view.viewmodel.VoteSongDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NowPlayingModule {
    @Provides
    @Singleton
    @Named("stations_error_response_converter")
    fun provideStationsErrorResponseConverter(retrofit: Retrofit): Converter<ResponseBody, StationsErrorResponse> {
        return retrofit.responseBodyConverter(StationsErrorResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    fun provideVoteSongDelegate(
        stationRepository: StationRepository,
        retrofit: Retrofit
    ): VoteSongDelegate {
        val converter: Converter<ResponseBody, VoteResponse> =
            retrofit.responseBodyConverter(VoteResponse::class.java, emptyArray())
        return VoteSongDelegate(stationRepository, converter)
    }
}

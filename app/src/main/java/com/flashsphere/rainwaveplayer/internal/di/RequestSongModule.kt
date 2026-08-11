package com.flashsphere.rainwaveplayer.internal.di

import com.flashsphere.rainwaveplayer.model.request.RequestSongResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Named
import jakarta.inject.Singleton
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object RequestSongModule {
    @Provides
    @Singleton
    @Named("request_song_converter")
    fun provideRequestSongConverter(retrofit: Retrofit): Converter<ResponseBody, RequestSongResponse> {
        return retrofit.responseBodyConverter(RequestSongResponse::class.java, emptyArray())
    }
}

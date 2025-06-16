package com.flashsphere.rainwaveplayer.internal.di

import com.flashsphere.rainwaveplayer.model.search.SearchResponse
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
object SearchModule {
    @Provides
    @Singleton
    @Named("search_converter")
    fun provideSearchConverter(retrofit: Retrofit): Converter<ResponseBody, SearchResponse> {
        return retrofit.responseBodyConverter(SearchResponse::class.java, emptyArray())
    }
}

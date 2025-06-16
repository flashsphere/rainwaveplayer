package com.flashsphere.rainwaveplayer.internal.di

import com.flashsphere.rainwaveplayer.model.request.ClearRequestsErrorResponse
import com.flashsphere.rainwaveplayer.model.request.DeleteRequestResponse
import com.flashsphere.rainwaveplayer.model.request.OrderRequestsResponse
import com.flashsphere.rainwaveplayer.model.request.PauseRequestResponse
import com.flashsphere.rainwaveplayer.model.request.RequestFaveResponse
import com.flashsphere.rainwaveplayer.model.request.RequestUnratedResponse
import com.flashsphere.rainwaveplayer.model.request.ResumeRequestResponse
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
object RequestsModule {
    @Provides
    @Singleton
    @Named("order_requests_converter")
    fun provideOrderRequestsConverter(retrofit: Retrofit): Converter<ResponseBody, OrderRequestsResponse> {
        return retrofit.responseBodyConverter(OrderRequestsResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    @Named("delete_request_converter")
    fun provideDeleteRequestConverter(retrofit: Retrofit): Converter<ResponseBody, DeleteRequestResponse> {
        return retrofit.responseBodyConverter(DeleteRequestResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    @Named("pause_request_converter")
    fun providePauseRequestConverter(retrofit: Retrofit): Converter<ResponseBody, PauseRequestResponse> {
        return retrofit.responseBodyConverter(PauseRequestResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    @Named("resume_request_converter")
    fun provideResumeRequestConverter(retrofit: Retrofit): Converter<ResponseBody, ResumeRequestResponse> {
        return retrofit.responseBodyConverter(ResumeRequestResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    @Named("clear_requests_converter")
    fun provideClearRequestsResponseConverter(retrofit: Retrofit): Converter<ResponseBody, ClearRequestsErrorResponse> {
        return retrofit.responseBodyConverter(ClearRequestsErrorResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    @Named("request_fave_converter")
    fun provideRequestFaveResponseConverter(retrofit: Retrofit): Converter<ResponseBody, RequestFaveResponse> {
        return retrofit.responseBodyConverter(RequestFaveResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    @Named("request_unrated_converter")
    fun provideRequestUnratedResponseConverter(retrofit: Retrofit): Converter<ResponseBody, RequestUnratedResponse> {
        return retrofit.responseBodyConverter(RequestUnratedResponse::class.java, emptyArray())
    }
}

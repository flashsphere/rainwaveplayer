package com.flashsphere.rainwaveplayer.util

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

fun readFile(clazz: Class<Any>, filePath: String): String {
    return clazz.getResource(filePath)!!.readText()
}

fun buildJson(): Json = Json {
    coerceInputValues = true
    ignoreUnknownKeys = true
}

fun buildRetrofit(mockWebServer: MockWebServer): Retrofit = Retrofit.Builder()
    .baseUrl(mockWebServer.url("/"))
    .addConverterFactory(buildJson().asConverterFactory("application/json".toMediaType()))
    .build()

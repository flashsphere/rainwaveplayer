package com.flashsphere.rainwaveplayer.model

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Connectivity
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Server
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Unauthorized
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Unknown
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import java.io.IOException

sealed interface ApiResponse<out T> {
    fun isSuccess() = this is SuccessApiResponse<*>

    companion object {
        fun <T> ofSuccess(data: T): ApiResponse<T> = SuccessApiResponse(data)
        fun ofFailure(throwable: Throwable): ApiResponse<Nothing> = FailureApiResponse(throwable)
    }
}

internal class SuccessApiResponse<out T>(
    val data: T,
) : ApiResponse<T>

internal class FailureApiResponse(
    val exception: Throwable,
) : ApiResponse<Nothing>

fun Throwable.toOperationError(): OperationError {
    return if (this is IOException) {
        OperationError(Connectivity, this.message)
    } else if (this is HttpException) {
        if (this.code() == 403) {
            OperationError(Unauthorized)
        } else {
            OperationError(Server)
        }
    } else {
        OperationError(Unknown)
    }
}

fun <T : HasResponseResult<*>> Throwable.toOperationError(converter: Converter<ResponseBody, T>): OperationError {
    return if (this is IOException) {
        OperationError(Connectivity, this.message)
    } else if (this is HttpException) {
        val responseResult = extractResponseResult(this, converter)
        if (this.code() == 403) {
            OperationError(Unauthorized, responseResult?.text, responseResult)
        } else {
            OperationError(Server, responseResult?.text, responseResult)
        }
    } else {
        OperationError(Unknown)
    }
}

private fun <T : HasResponseResult<*>> extractResponseResult(
    e: HttpException,
    converter: Converter<ResponseBody, T>
): ResponseResult? {
    runCatching {
        e.response()?.errorBody()?.use { responseBody ->
            return converter.convert(responseBody)?.result
        }
    }
    return null
}

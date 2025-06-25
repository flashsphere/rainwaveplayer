package com.flashsphere.rainwaveplayer.util

import android.content.Context
import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.ResponseResult

@Immutable
class OperationError(
    @OperationErrorDef val type: Int,
    val message: String? = null,
    val responseResult: ResponseResult? = null,
) {

    @IntDef(Unknown, Connectivity, Unauthorized, Server)
    @Retention(AnnotationRetention.SOURCE)
    annotation class OperationErrorDef

    fun getMessage(context: Context, defaultMessage: String): String {
        return when (type) {
            Connectivity -> {
                context.getString(R.string.error_connection)
            }
            Unauthorized -> {
                message ?: defaultMessage
            }
            Server -> {
                message ?: defaultMessage
            }
            else -> {
                defaultMessage
            }
        }
    }

    companion object {
        const val Unknown = 0
        const val Connectivity = 1
        const val Unauthorized = 2
        const val Server = 3
    }
}

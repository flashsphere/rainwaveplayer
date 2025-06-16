package com.flashsphere.rainwaveplayer.util

data class UserCredentials(
    val userId: Int,
    val apiKey: String,
)

fun UserCredentials?.isLoggedIn(): Boolean {
    return this != null
}

package com.flashsphere.rainwaveplayer.model

interface HasResponseResult<T : ResponseResult> {
    val result: T?
}

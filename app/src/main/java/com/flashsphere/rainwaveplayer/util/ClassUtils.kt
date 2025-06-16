package com.flashsphere.rainwaveplayer.util

import kotlin.reflect.KClass

object ClassUtils {

    fun getSimpleName(obj: KClass<*>): String {
        return obj.java.simpleName
    }

    fun Any.getSimpleClassName(): String {
        return javaClass.simpleName
    }

    fun KClass<*>.getSimpleClassName(): String {
        return java.simpleName
    }

    fun getName(obj: KClass<*>): String {
        return obj.java.name
    }

    fun getName(obj: Any): String {
        return obj::class.java.name
    }

    fun Any.getClassName(): String {
        return javaClass.name
    }

    fun KClass<*>.getClassName(): String {
        return java.name
    }
}

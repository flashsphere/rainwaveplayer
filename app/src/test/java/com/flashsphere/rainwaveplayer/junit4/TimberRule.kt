package com.flashsphere.rainwaveplayer.junit4

import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class TimberRule : TestWatcher() {

    private val printlnTree = object : Timber.DebugTree() {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val thread = Thread.currentThread()
            val type = when (priority) {
                Log.VERBOSE -> 'V'
                Log.DEBUG -> 'D'
                Log.INFO -> 'I'
                Log.WARN -> 'W'
                Log.ERROR -> 'E'
                else -> 'E'
            }

            val logMessage = StringBuilder()
                .append(dateTimeFormatter.format(LocalDateTime.now()))
                .append(" ")
                .append(thread.threadId())
                .append("/")
                .append(thread.name.take(5))
                .append("...")
                .append(thread.name.takeLast(8))
                .append(" ")
                .append(type)
                .append("/")
                .append(tag)
                .append(": ")
                .append(message)
                .toString()

            println(logMessage)
        }
    }

    override fun starting(description: Description) {
        super.starting(description)
        Timber.plant(printlnTree)
    }

    override fun finished(description: Description) {
        Timber.uproot(printlnTree)
        super.finished(description)
    }
}

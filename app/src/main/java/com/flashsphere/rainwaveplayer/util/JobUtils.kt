package com.flashsphere.rainwaveplayer.util

import kotlinx.coroutines.Job

object JobUtils {
    fun cancel(vararg jobs: Job?) {
        for (job in jobs) {
            job?.cancel()
        }
    }
    fun cancel(jobs: List<Job>) {
        for (job in jobs) {
            job.cancel()
        }
    }
}

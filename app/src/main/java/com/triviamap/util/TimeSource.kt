package com.triviamap.util

import android.os.SystemClock

/** Monotonic clock, injectable so game timing can be tested. */
fun interface TimeSource {
    fun elapsedMs(): Long

    companion object {
        val System = TimeSource { SystemClock.elapsedRealtime() }
    }
}

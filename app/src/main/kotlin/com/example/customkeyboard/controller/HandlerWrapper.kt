package com.example.customkeyboard.controller

/**
 * Abstraction over [android.os.Handler] to allow [KeyboardController] to be tested on the JVM
 * without any Android framework dependencies.
 */
interface HandlerWrapper {
    /** Posts [runnable] to be run after [delayMs] milliseconds. */
    fun postDelayed(runnable: Runnable, delayMs: Long)

    /** Removes any pending posts of [runnable] from the message queue. */
    fun removeCallbacks(runnable: Runnable)
}

/**
 * Production implementation that delegates to [android.os.Handler] on the main looper.
 */
class DefaultHandlerWrapper : HandlerWrapper {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun postDelayed(runnable: Runnable, delayMs: Long) {
        handler.postDelayed(runnable, delayMs)
    }

    override fun removeCallbacks(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }
}

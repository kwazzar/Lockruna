package com.kwazzar.lockruna.domain.session

/**
 * Auto-lock timeout policy: how long the vault may stay unlocked while backgrounded.
 * The decision is made from monotonic time, so changing the wall clock never triggers a lock.
 */
data class AutoLockPolicy(val timeoutMillis: Long) {
    fun isExpired(backgroundedAtMonotonicMillis: Long, nowMonotonicMillis: Long): Boolean =
        nowMonotonicMillis - backgroundedAtMonotonicMillis >= timeoutMillis

    companion object {
        const val IMMEDIATE_MILLIS = 0L
        const val ONE_MINUTE_MILLIS = 60_000L
        const val FIVE_MINUTES_MILLIS = 5 * 60_000L
        const val FIFTEEN_MINUTES_MILLIS = 15 * 60_000L

        val IMMEDIATE = AutoLockPolicy(IMMEDIATE_MILLIS)
        val ONE_MINUTE = AutoLockPolicy(ONE_MINUTE_MILLIS)
        val FIVE_MINUTES = AutoLockPolicy(FIVE_MINUTES_MILLIS)
        val FIFTEEN_MINUTES = AutoLockPolicy(FIFTEEN_MINUTES_MILLIS)

        /** The choices the settings screen offers (US-08). */
        val ALL = listOf(IMMEDIATE, ONE_MINUTE, FIVE_MINUTES, FIFTEEN_MINUTES)
    }
}

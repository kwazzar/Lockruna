package com.kwazzar.lockruna.data.db

import com.kwazzar.lockruna.domain.session.Clock

class FakeClock(var wallMillis: Long = 0, var monotonicMillis: Long = 0) : Clock {
    override fun nowMillis(): Long = wallMillis
    override fun monotonicMillis(): Long = monotonicMillis
}

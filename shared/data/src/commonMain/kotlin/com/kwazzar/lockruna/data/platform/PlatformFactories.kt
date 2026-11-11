package com.kwazzar.lockruna.data.platform

import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import io.ktor.client.HttpClient

/** Wall and monotonic clock of the platform. */
expect fun platformClock(): Clock

/** UUID source of the platform. */
expect fun platformIdGenerator(): IdGenerator

/** HTTP client of the platform, with JSON and sane timeouts. */
expect fun platformHttpClient(): HttpClient

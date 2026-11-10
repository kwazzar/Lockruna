package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.sync.AccessTokenProvider

/** Access token stub; null means the session is not authenticated. */
class FakeAccessTokenProvider(var tokenValue: String? = "access-token") : AccessTokenProvider {
    override suspend fun token(): String? = tokenValue
}

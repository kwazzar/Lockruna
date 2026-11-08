package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.platform.error.unauthorized
import java.time.Clock
import java.time.Duration

/**
 * Rotates a refresh token: the presented one is spent, a new pair of the same family is issued.
 * Reusing a spent token revokes the whole family.
 */
class RefreshSession(
    private val refreshTokens: RefreshTokenRepository,
    private val tokens: TokenService,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(refreshToken: String): TokenPair {
        val now = clock.instant()
        val stored = refreshTokens.findByHash(AuthKeys.tokenHash(refreshToken)) ?: throw unauthorized()
        val spent = stored.usedAt != null || stored.revokedAt != null
        val expired = stored.familyCreatedAt.plus(FAMILY_LIFETIME) < now || stored.expiresAt < now
        if (spent || expired) {
            if (spent) refreshTokens.revokeFamily(stored.userId, stored.familyId, now)
            throw unauthorized()
        }

        refreshTokens.markUsed(stored.userId, stored.id, now)
        return TokenPair(
            accessToken = tokens.issueAccessToken(stored.userId, stored.deviceId),
            refreshToken = tokens.issueRefreshToken(
                stored.userId,
                stored.deviceId,
                stored.familyId,
                stored.familyCreatedAt,
            ),
            expiresIn = ACCESS_TOKEN_SECONDS,
        )
    }
}

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresIn: Int)

private val FAMILY_LIFETIME: Duration = Duration.ofDays(90)

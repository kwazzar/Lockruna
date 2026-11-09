package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
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
    private val transactions: TransactionRunner,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(refreshToken: String): TokenPair {
        val outcome = transactions.transaction { spendAndRotate(refreshToken) }
        return when (outcome) {
            is Rotation.Issued -> outcome.pair
            is Rotation.Reused -> {
                // The revocation is committed on its own: a failed refresh must not roll the family back alive.
                refreshTokens.revokeFamily(outcome.userId, outcome.familyId, clock.instant())
                throw unauthorized()
            }
            is Rotation.Rejected -> throw unauthorized()
        }
    }

    /**
     * Spends the token and issues the new pair in one transaction, so two concurrent refreshes of the same
     * token cannot both succeed: the second one finds the token spent.
     */
    private suspend fun spendAndRotate(refreshToken: String): Rotation {
        val stored = refreshTokens.findByHash(AuthKeys.tokenHash(refreshToken)) ?: return Rotation.Rejected
        val now = clock.instant()
        val reused = stored.usedAt != null || stored.revokedAt != null
        val expired = stored.familyCreatedAt.plus(FAMILY_LIFETIME) < now || stored.expiresAt < now
        if (reused || expired) {
            return if (reused) Rotation.Reused(stored.userId, stored.familyId) else Rotation.Rejected
        }
        if (!refreshTokens.markUsed(stored.userId, stored.id, now)) {
            return Rotation.Reused(stored.userId, stored.familyId)
        }

        return Rotation.Issued(
            TokenPair(
                accessToken = tokens.issueAccessToken(stored.userId, stored.deviceId),
                refreshToken = tokens.issueRefreshToken(
                    stored.userId,
                    stored.deviceId,
                    stored.familyId,
                    stored.familyCreatedAt,
                ),
                expiresIn = ACCESS_TOKEN_SECONDS,
            ),
        )
    }
}

sealed interface Rotation {
    data class Issued(val pair: TokenPair) : Rotation

    data class Reused(val userId: String, val familyId: String) : Rotation

    data object Rejected : Rotation
}

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresIn: Int)

private val FAMILY_LIFETIME: Duration = Duration.ofDays(90)

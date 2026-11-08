package com.kwazzar.lockruna.server.identity.infrastructure

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AccessTokenClaims
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRecord
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * Access tokens are HS256 JWTs; refresh tokens are random bytes of which only the SHA-256 hash is stored.
 */
class JwtTokenService(
    private val refreshTokens: RefreshTokenRepository,
    private val settings: ServerSettings,
    private val clock: Clock = Clock.systemUTC(),
) : TokenService {
    private val activeAlgorithm = Algorithm.HMAC256(settings.jwtActiveKey)
    private val previousAlgorithm = settings.jwtPreviousKey?.let { Algorithm.HMAC256(it) }

    override fun issueAccessToken(userId: String, deviceId: String): String {
        val now = clock.instant()
        return JWT.create()
            .withIssuer(settings.jwtIssuer)
            .withAudience(settings.jwtAudience)
            .withSubject(userId)
            .withClaim(DEVICE_ID_CLAIM, deviceId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(ACCESS_TOKEN_SECONDS.toLong())))
            .sign(activeAlgorithm)
    }

    override suspend fun issueRefreshToken(
        userId: String,
        deviceId: String,
        familyId: String,
        familyCreatedAt: Instant,
    ): String {
        val token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(REFRESH_TOKEN_BYTES).also(SecureRandom()::nextBytes))
        refreshTokens.insert(
            RefreshTokenRecord(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId,
                userId = userId,
                familyId = familyId,
                tokenHash = AuthKeys.tokenHash(token),
                expiresAt = clock.instant().plus(REFRESH_TOKEN_LIFETIME),
                familyCreatedAt = familyCreatedAt,
                usedAt = null,
                revokedAt = null,
            ),
        )
        return token
    }

    override fun verifyAccessToken(token: String): AccessTokenClaims? {
        val decoded = try {
            verify(token)
        } catch (ignored: JWTVerificationException) {
            return null
        }
        val deviceId = decoded.getClaim(DEVICE_ID_CLAIM).asString() ?: return null
        return AccessTokenClaims(userId = decoded.subject, deviceId = deviceId)
    }

    private fun verify(token: String): DecodedJWT = try {
        verifier(activeAlgorithm).verify(token)
    } catch (failure: JWTVerificationException) {
        val previous = previousAlgorithm ?: throw failure
        verifier(previous).verify(token)
    }

    private fun verifier(algorithm: Algorithm): JWTVerifier = JWT.require(algorithm)
        .withIssuer(settings.jwtIssuer)
        .withAudience(settings.jwtAudience)
        .build()

    private companion object {
        const val DEVICE_ID_CLAIM = "deviceId"
        const val REFRESH_TOKEN_BYTES = 32
        val REFRESH_TOKEN_LIFETIME: Duration = Duration.ofDays(30)
    }
}

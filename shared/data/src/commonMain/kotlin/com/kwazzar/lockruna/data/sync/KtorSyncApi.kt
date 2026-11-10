package com.kwazzar.lockruna.data.sync

import com.kwazzar.lockruna.contract.PushRequestDto
import com.kwazzar.lockruna.contract.PushResultDto
import com.kwazzar.lockruna.contract.SyncPageDto
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.PushResult
import com.kwazzar.lockruna.domain.sync.SyncApi
import com.kwazzar.lockruna.domain.sync.SyncPage
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

private const val IDEMPOTENCY_HEADER = "Idempotency-Key"

/**
 * Sync transport over HTTP.
 *
 * The client must have ContentNegotiation with kotlinx JSON installed.
 */
class KtorSyncApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val accessTokenProvider: AccessTokenProvider,
) : SyncApi {

    override suspend fun pull(cursor: String?): Result<SyncPage, AppError> {
        val token = accessTokenProvider.token() ?: return Result.Failure(AppError.Unauthorized)
        return network {
            val response = httpClient.get("$baseUrl/sync") {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (cursor != null) parameter("cursor", cursor)
            }
            respond(response) { response.body<SyncPageDto>().toDomain() }
        }
    }

    override suspend fun push(changes: List<PendingChange>, idempotencyKey: String): Result<PushResult, AppError> {
        val token = accessTokenProvider.token() ?: return Result.Failure(AppError.Unauthorized)
        return network {
            val response = httpClient.post("$baseUrl/sync/push") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(IDEMPOTENCY_HEADER, idempotencyKey)
                contentType(ContentType.Application.Json)
                setBody(PushRequestDto(changes = changes.map { it.toDto() }))
            }
            respond(response) { response.body<PushResultDto>().toDomain() }
        }
    }

    private suspend fun <T> respond(response: HttpResponse, body: suspend () -> T): Result<T, AppError> =
        when (response.status) {
            HttpStatusCode.OK -> Result.Success(body())
            HttpStatusCode.Conflict -> Result.Failure(AppError.IdempotencyConflict)
            HttpStatusCode.Unauthorized -> Result.Failure(AppError.Unauthorized)
            HttpStatusCode.TooManyRequests ->
                Result.Failure(AppError.RateLimited(response.headers[HttpHeaders.RetryAfter]?.toIntOrNull() ?: 0))
            else -> Result.Failure(AppError.NetworkUnavailable)
        }

    private suspend fun <T> network(block: suspend () -> Result<T, AppError>): Result<T, AppError> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (ignored: HttpRequestTimeoutException) {
        Result.Failure(AppError.NetworkUnavailable)
    } catch (ignored: IOException) {
        Result.Failure(AppError.NetworkUnavailable)
    } catch (ignored: ContentConvertException) {
        Result.Failure(AppError.NetworkUnavailable)
    } catch (ignored: NoTransformationFoundException) {
        Result.Failure(AppError.NetworkUnavailable)
    } catch (ignored: IllegalArgumentException) {
        Result.Failure(AppError.NetworkUnavailable)
    }
}

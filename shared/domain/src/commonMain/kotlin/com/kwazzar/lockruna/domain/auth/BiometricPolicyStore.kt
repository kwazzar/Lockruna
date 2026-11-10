package com.kwazzar.lockruna.domain.auth

interface BiometricPolicyStore {
    suspend fun failures(): Int
    suspend fun recordFailure()
    suspend fun resetFailures()
    suspend fun setLastSuccessfulUnlock(millis: Long)
    suspend fun lastSuccessfulUnlock(): Long?
}
package com.kwazzar.lockruna.umbrella

/**
 * Entry point of the shared framework for the native apps.
 *
 * The native side only sees this API; persistence, crypto and sync stay inside
 * the Kotlin core. The API grows together with the use cases it exposes.
 */
object SharedCore {
    /** Version of the shared Kotlin core. */
    const val VERSION: String = "0.1.0"
}

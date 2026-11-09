package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The KDF burns 64 MiB of memory and CPU, so it must not run in the thread of
 * the caller: on iOS a main-thread call would freeze the UI and trip the
 * watchdog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Argon2DispatchTest {
    @Test
    fun `argon2id does not block the calling dispatcher`() = runBlocking {
        val dispatcher = newSingleThreadContext("kdf-caller")
        try {
            val argon2 = Argon2Factory.create()
            val order = Collections.synchronizedList(mutableListOf<String>())

            withTimeout(30_000) {
                launch(dispatcher) {
                    // Queued on the same single thread: it can only run while the
                    // KDF yields the thread.
                    launch(dispatcher) { order.add("probe") }
                    argon2.hash("password", ByteArray(KdfParams.DEFAULT_SALT_LENGTH), KdfParams())
                    order.add("hash")
                }.join()
            }

            assertEquals(listOf("probe", "hash"), order)
        } finally {
            dispatcher.close()
        }
    }
}

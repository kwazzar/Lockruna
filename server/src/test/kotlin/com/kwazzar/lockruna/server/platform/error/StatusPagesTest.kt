package com.kwazzar.lockruna.server.platform.error

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatusPagesTest {
    private val logger = LoggerFactory.getLogger("com.kwazzar.lockruna.server.error") as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeTest
    fun attach() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterTest
    fun detach() {
        logger.detachAppender(appender)
    }

    @Test
    fun `an unexpected failure is logged without the exception message`() = testApplication {
        application {
            installStatusPages()
            routing {
                get("/boom") { throw IllegalStateException("secret-value") }
            }
        }

        val response = client.get("/boom")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val logged = appender.list.joinToString(" ") { it.formattedMessage }
        assertTrue(logged.contains("IllegalStateException"), logged)
        assertFalse(logged.contains("secret-value"), logged)
    }
}

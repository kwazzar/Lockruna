package com.kwazzar.lockruna.server.identity

import com.kwazzar.lockruna.contract.DeviceDto
import com.kwazzar.lockruna.server.module
import com.kwazzar.lockruna.server.support.IdentityClient
import com.kwazzar.lockruna.server.support.InMemoryDeviceDenylist
import com.kwazzar.lockruna.server.support.InMemoryRateLimiter
import com.kwazzar.lockruna.server.support.PostgresTest
import com.kwazzar.lockruna.server.support.json
import com.kwazzar.lockruna.server.support.randomBytes
import com.kwazzar.lockruna.server.support.testSettings
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceApiTest {
    private val settings = testSettings()

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `devices lists the own devices and marks the current one`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        val registered = client.register("user@example.com", authKey).json()
        val firstDevice = registered["deviceId"]!!.jsonPrimitive.content
        val login = client.login("user@example.com", authKey).json()
        val secondDevice = login["deviceId"]!!.jsonPrimitive.content

        val response = client.devices(login["accessToken"]!!.jsonPrimitive.content)

        assertEquals(HttpStatusCode.OK, response.status)
        val devices = Json.decodeFromString<List<DeviceDto>>(response.bodyAsText())
        assertEquals(2, devices.size)
        assertEquals(secondDevice, devices.single { it.current }.id)
        assertEquals(firstDevice, devices.single { !it.current }.id)
    }

    @Test
    fun `deleting a device blocks its access token immediately`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        val registered = client.register("user@example.com", authKey).json()
        val accessToken = registered["accessToken"]!!.jsonPrimitive.content
        val login = client.login("user@example.com", authKey).json()
        val otherToken = login["accessToken"]!!.jsonPrimitive.content

        val deleted = client.deleteDevice(otherToken, registered["deviceId"]!!.jsonPrimitive.content)

        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals(HttpStatusCode.Unauthorized, client.devices(accessToken).status)
        val remaining = Json.decodeFromString<List<DeviceDto>>(client.devices(otherToken).bodyAsText())
        assertEquals(listOf(login["deviceId"]!!.jsonPrimitive.content), remaining.map { it.id })
    }

    @Test
    fun `a device of another user is not revoked`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val owner = client.register("owner@example.com", randomBytes(32)).json()
        val other = client.register("other@example.com", randomBytes(32)).json()
        val ownerToken = owner["accessToken"]!!.jsonPrimitive.content
        val otherToken = other["accessToken"]!!.jsonPrimitive.content

        val deleted = client.deleteDevice(ownerToken, other["deviceId"]!!.jsonPrimitive.content)

        assertEquals(HttpStatusCode.NotFound, deleted.status)
        assertEquals(HttpStatusCode.OK, client.devices(otherToken).status)
        val ownerDevices = Json.decodeFromString<List<DeviceDto>>(client.devices(ownerToken).bodyAsText())
        assertEquals(listOf(owner["deviceId"]!!.jsonPrimitive.content), ownerDevices.map { it.id })
    }
}

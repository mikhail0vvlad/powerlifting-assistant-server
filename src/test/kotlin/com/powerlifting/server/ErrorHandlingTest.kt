package com.powerlifting.server

import com.powerlifting.server.domain.error.NotFoundException
import com.powerlifting.server.domain.error.UnauthorizedException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверяет контракт ошибок, описанный в docs/api.md, без БД и без Firebase:
 * поднимается только installErrorHandling + пара роутов, которые бросают исключения.
 */
class ErrorHandlingTest {

    private fun testApp(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        application {
            // explicitNulls = false must match production's Json config
            // (Application.kt) — otherwise details:null leaks into responses
            // that never set it.
            install(ContentNegotiation) { json(Json { explicitNulls = false }) }
            installErrorHandling(appEnv = "production")
            routing {
                get("/unauth") { throw UnauthorizedException("Missing Authorization header") }
                get("/missing") { throw NotFoundException("no such row") }
                get("/bad") { throw IllegalArgumentException("weeks must be positive") }
                get("/boom") { throw IllegalStateException("jdbc connection reset") }
            }
        }
        block(client)
    }

    @Test
    fun `unauthorized maps to 401 with error unauthorized`() = testApp { client ->
        val response = client.get("/unauth")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("""{"error":"unauthorized"}""", response.bodyAsText())
    }

    @Test
    fun `not found maps to 404 without leaking the reason`() = testApp { client ->
        val response = client.get("/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue("no such row" !in response.bodyAsText())
    }

    @Test
    fun `illegal argument maps to 400 with details`() = testApp { client ->
        val response = client.get("/bad")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue("weeks must be positive" in response.bodyAsText())
    }

    @Test
    fun `internal error hides cause outside development`() = testApp { client ->
        val response = client.get("/boom")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue("jdbc" !in response.bodyAsText())
    }
}

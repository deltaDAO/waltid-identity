package id.walt.verifier

import io.ktor.http.Headers
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VerifierApiStateIdExtractionTest {

    @Test
    fun shouldPreferStateIdHeaderOverBody() {
        val headers: Headers = headersOf("stateId", "header-session")
        val body = buildJsonObject {
            put("stateId", "body-session")
            put("sessionId", "body-session-id")
        }

        assertEquals("header-session", extractStateId(headers, body))
    }

    @Test
    fun shouldFallbackToSessionIdFromBodyWhenHeaderMissing() {
        val headers: Headers = headersOf()
        val body = buildJsonObject {
            put("sessionId", "body-session-id")
        }

        assertEquals("body-session-id", extractStateId(headers, body))
    }

    @Test
    fun shouldFallbackToSnakeCaseSessionIdFromBodyWhenHeaderMissing() {
        val headers: Headers = headersOf()
        val body = buildJsonObject {
            put("session_id", "body-session-id")
        }

        assertEquals("body-session-id", extractStateId(headers, body))
    }

    @Test
    fun shouldReturnNullWhenNoStateIdentifiersProvided() {
        val headers: Headers = headersOf()
        val body = buildJsonObject {
            put("request_credentials", "[]")
        }

        assertNull(extractStateId(headers, body))
    }
}

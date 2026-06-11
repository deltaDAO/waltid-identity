package id.walt.oid4vc

import id.walt.oid4vc.responses.TokenResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenResponseHttpParameterTest {
    @Test
    fun testParseVpTokenJsonArrayFromHttpParameters() {
        val jwt1 = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIxIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val jwt2 = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIyIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val response = TokenResponse.fromHttpParameters(
            mapOf("vp_token" to listOf("[\"$jwt1\",\"$jwt2\"]"))
        )

        assertTrue(response.vpToken is JsonArray)
        val array = response.vpToken as JsonArray
        assertEquals(jwt1, array[0].jsonPrimitive.content)
        assertEquals(jwt2, array[1].jsonPrimitive.content)
    }

    @Test
    fun testParseVpTokenRawJwtFromHttpParameters() {
        val jwt = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIxIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val response = TokenResponse.fromHttpParameters(mapOf("vp_token" to listOf(jwt)))

        assertTrue(response.vpToken is JsonPrimitive)
        assertEquals(jwt, response.vpToken!!.jsonPrimitive.content)
    }

    @Test
    fun testParseVpTokenQuotedJsonStringFromHttpParameters() {
        val jwt = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIxIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val rawJsonString = JsonPrimitive(jwt).toString()
        val response = TokenResponse.fromHttpParameters(mapOf("vp_token" to listOf(rawJsonString)))

        assertTrue(response.vpToken is JsonPrimitive)
        assertEquals(jwt, response.vpToken!!.jsonPrimitive.content)
    }
}

package id.walt.ktorauthnz.methods

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.MACVerifier
import id.walt.commons.web.JWTVerificationException
import id.walt.crypto.keys.jwk.JwkKeyProvider
import id.walt.crypto.utils.JwsUtils.decodeJws
import id.walt.ktorauthnz.AuthContext
import id.walt.ktorauthnz.accounts.identifiers.methods.JWTIdentifier
import id.walt.ktorauthnz.amendmends.AuthMethodFunctionAmendments
import id.walt.ktorauthnz.exceptions.authCheck
import id.walt.ktorauthnz.methods.config.JwtAuthConfiguration
import id.walt.ktorauthnz.sessions.AuthSessionInformation
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.time.Clock
import kotlin.time.Instant

object JWT : AuthenticationMethod("jwt") {

    private val keyProviderCache = mutableMapOf<String, JwkKeyProvider>()
    private fun getJwkProvider(jwksUrl: String) = keyProviderCache.getOrPut(jwksUrl) { JwkKeyProvider(jwksUrl) }

    suspend fun auth(jwt: String, config: JwtAuthConfiguration): JWTIdentifier {
        val id = when {
            config.jwksUrl != null -> authWithJwks(jwt, config)
            config.verifyKey != null -> authWithHmac(jwt, config)
            else -> error("JwtAuthConfiguration requires either 'jwksUrl' or 'verifyKey'")
        }
        return JWTIdentifier(id)
    }

    private fun authWithHmac(jwt: String, config: JwtAuthConfiguration): String {
        val parsedJws = JWSObject.parse(jwt)
        authCheck(parsedJws.verify(MACVerifier(config.verifyKey)), JWTVerificationException())
        return parsedJws.payload.toJSONObject()[config.identifyClaim] as String
    }

    private suspend fun authWithJwks(jwt: String, config: JwtAuthConfiguration): String {
        val jwsParts = jwt.decodeJws()
        val header = jwsParts.header
        val payload = jwsParts.payload

        val kid = header["kid"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("JWT header is missing 'kid'.")

        val keyProvider = getJwkProvider(config.jwksUrl!!)
        val key = keyProvider.getKey(kid).getOrThrow()

        val verificationResult = key.verifyJws(jwt)
        authCheck(verificationResult.isSuccess, JWTVerificationException())

        config.issuer?.let { expectedIss ->
            val iss = payload["iss"]?.jsonPrimitive?.content
            require(iss == expectedIss) { "Invalid issuer: expected '$expectedIss', got '$iss'" }
        }

        val exp = payload["exp"]?.jsonPrimitive?.long ?: 0
        require(Instant.fromEpochSeconds(exp) >= Clock.System.now()) { "JWT has expired." }

        return payload[config.identifyClaim]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("JWT is missing '${config.identifyClaim}' claim.")
    }

    override fun Route.registerAuthenticationRoutes(
        authContext: ApplicationCall.() -> AuthContext,
        functionAmendments: Map<AuthMethodFunctionAmendments, suspend (Any) -> Unit>?
    ) {
        post("jwt", {
            request { body<String> { required = true } }
            response { HttpStatusCode.OK to { body<AuthSessionInformation>() } }
        }) {
            val session = call.getAuthSession(authContext)
            val config = session.lookupFlowMethodConfiguration<JwtAuthConfiguration>(this@JWT)

            val jwt = call.receiveText()
            val id = auth(jwt, config)

            val authContext = authContext(call)
            call.handleAuthSuccess(session, authContext, id.resolveToAccountId())
        }
    }
}

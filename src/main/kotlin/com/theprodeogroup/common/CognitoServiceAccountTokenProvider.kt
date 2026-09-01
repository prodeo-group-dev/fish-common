package com.theprodeogroup.common

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
private data class InitiateAuthRequest(
    val AuthFlow: String,
    val ClientId: String,
    val AuthParameters: Map<String, String>
)

@Serializable
private data class InitiateAuthResponse(val AuthenticationResult: AuthenticationResultDto? = null)

@Serializable
private data class AuthenticationResultDto(val IdToken: String, val ExpiresIn: Long)

/**
 * Logs into Cognito as a service-account identity, caching the
 * resulting ID token and refreshing it before it expires - moved here
 * 2026-09-01 ("scope out how POP's receive-line would call IM") after
 * being independently hand-copied, byte-identical, into SOP, IM, and
 * (almost) POP: SOP's own call into GL, IM's own call into GL, and now
 * POP's own call into IM all need exactly this. Same class this whole
 * platform already exists to prevent duplicating (see [Money]'s own
 * KDoc) - the fourth copy is what finally triggered moving it here
 * instead of writing a fifth someday.
 *
 * A dedicated service-account Cognito user, not a client-credentials
 * (M2M) flow - every caller's `Auth.kt` on the receiving side verifies
 * ID tokens (checks `aud`/`email` claims), and client_credentials only
 * ever issues an access token, which has neither. Calls `InitiateAuth`
 * directly over HTTPS (no AWS SDK dependency).
 *
 * Exposed via [invoke] as a plain, non-suspending `() -> String` to
 * match every gateway's `bearerTokenProvider` contract exactly.
 *
 * **Not yet retrofitted into SOP/IM's own existing copies** - those
 * still carry their own independent, now-superseded versions. Flagged
 * as follow-up cleanup, not done in the same pass as this file's own
 * introduction.
 */
class CognitoServiceAccountTokenProvider(
    private val client: HttpClient,
    private val region: String,
    private val clientId: String,
    private val clientSecret: String,
    private val username: String,
    private val password: String
) {
    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var expiresAt: Instant = Instant.EPOCH

    private val refreshBuffer: Duration = Duration.ofMinutes(2)

    operator fun invoke(): String {
        val token = cachedToken
        if (token != null && Instant.now().isBefore(expiresAt.minus(refreshBuffer))) {
            return token
        }
        return runBlocking { refresh() }
    }

    private suspend fun refresh(): String {
        val requestBody = Json.encodeToString(
            InitiateAuthRequest.serializer(),
            InitiateAuthRequest(
                AuthFlow = "USER_PASSWORD_AUTH",
                ClientId = clientId,
                AuthParameters = mapOf(
                    "USERNAME" to username,
                    "PASSWORD" to password,
                    "SECRET_HASH" to computeSecretHash()
                )
            )
        )

        val response = client.post("https://cognito-idp.$region.amazonaws.com/") {
            contentType(ContentType.parse("application/x-amz-json-1.1"))
            header("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth")
            setBody(requestBody)
        }

        val parsed = Json.decodeFromString(InitiateAuthResponse.serializer(), response.bodyAsText())
        val result = parsed.AuthenticationResult
            ?: error("Cognito InitiateAuth for $username returned no AuthenticationResult (HTTP ${response.status})")

        cachedToken = result.IdToken
        expiresAt = Instant.now().plusSeconds(result.ExpiresIn)
        return result.IdToken
    }

    /** Required whenever the app client has a secret (deliberately, on every service-account app client this platform provisions) - HMAC-SHA256(clientSecret, username + clientId), base64-encoded. */
    private fun computeSecretHash(): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(clientSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal((username + clientId).toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(raw)
    }
}

package io.eyecu.passhelper.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimNames
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.jwt.proc.JWTProcessor
import io.eyecu.passhelper.util.jacksonObjectMapper
import io.eyecu.passhelper.web.Body
import io.eyecu.passhelper.web.Path
import io.eyecu.passhelper.web.Response
import io.eyecu.passhelper.web.StatusCode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.HttpCookie
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.util.Base64

class CognitoService(
    awsRegion: String,
    userPoolId: String,
    clientId: String,
    clientSecret: String,
    cognitoHostedUiUrl: String,
    domainName: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val jwtProcessor: JWTProcessor<SecurityContext> = defaultJwtProcessor(awsRegion, userPoolId),
) {

    companion object {
        private val logger = KotlinLogging.logger {}

        private fun defaultJwtProcessor(awsRegion: String, userPoolId: String) =
            DefaultJWTProcessor<SecurityContext>().apply {
                val cognitoJwksUrl = "https://cognito-idp.$awsRegion.amazonaws.com/$userPoolId/.well-known/jwks.json"
                val expectedIssuer = "https://cognito-idp.$awsRegion.amazonaws.com/$userPoolId"

                val source = JWKSourceBuilder.create<SecurityContext>(
                    URI.create(cognitoJwksUrl).toURL()
                ).retrying(true)
                    .build()

                jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, source)
                setJWTClaimsSetVerifier(
                    DefaultJWTClaimsVerifier(
                        JWTClaimsSet.Builder().issuer(expectedIssuer).build(),
                        setOf(
                            JWTClaimNames.SUBJECT,
                            JWTClaimNames.ISSUED_AT,
                            JWTClaimNames.EXPIRATION_TIME
                        )
                    )
                )
            }
    }

    private val loginUrl = URLEncoder.encode("https://$domainName/login", Charsets.UTF_8)

    private val cognitoLoginUrl =
        "https://$cognitoHostedUiUrl/login?client_id=$clientId&redirect_uri=$loginUrl&response_type=code&scope=openid"

    private val cognitoTokenDomain = "https://$cognitoHostedUiUrl/oauth2/token"

    private val authHeader =
        String(Base64.getEncoder().encode("$clientId:$clientSecret".encodeToByteArray()), Charsets.UTF_8)

    val cognitoLoginRedirectResponse =
        Response(
            StatusCode(302),
            Path(cognitoLoginUrl),
            Body(null),
            emptyList()
        )

    fun isAuthenticated(requestCookies: List<String>?): Boolean {
        val accessToken = requestCookies?.getAccessTokenCookie() ?: return false

        if (accessToken.isBlank()) {
            return false
        }

        return try {
            jwtProcessor.process(accessToken, null)
                .claims
                .isNotEmpty()
        } catch (e: Exception) {
            logger.error(e) { }
            false
        }
    }

    fun authenticate(code: String): Jwt? {
        val params = mapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to loginUrl,
        )

        val request = HttpRequest.newBuilder(URI.create(cognitoTokenDomain))
            .header("content-type", "application/x-www-form-urlencoded")
            .headers("authorization", "Basic $authHeader")
            .POST(
                BodyPublishers.ofString(
                    params.map { (key, value) ->
                        "$key=$value"
                    }.joinToString(separator = "&")
                )
            ).build()

        val result = httpClient.send(request, BodyHandlers.ofString())

        if (result.statusCode() != 200) {
            return null
        }
        val body = result.body()

        return jacksonObjectMapper.readValue<Jwt>(body)
    }

    private fun List<String>.getAccessTokenCookie(): String? = map {
        HttpCookie.parse(it)
    }.flatten().firstOrNull {
        it.name == "accessToken"
    }?.value
}

data class Jwt(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Int
)
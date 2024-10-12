import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.JWTProcessor
import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.web.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

class CognitoServiceTest {

    private val httpClient = mock<HttpClient>()
    private val jwtProcessor = mock<JWTProcessor<SecurityContext>>()

    private val cognitoService = CognitoService(
        awsRegion = "us-west-2",
        userPoolId = "testPoolId",
        clientId = "testClientId",
        clientSecret = "testClientSecret",
        cognitoHostedUiUrl = "cognito.testhost.com",
        domainName = "testdomain.com",
        httpClient = httpClient,
        jwtProcessor = jwtProcessor
    )

    @Test
    fun `should return false when cookies are null in isAuthenticated`() {
        assertFalse(cognitoService.isAuthenticated(null))
    }

    @Test
    fun `should return false when accessToken is blank in isAuthenticated`() {
        val cookies = listOf("accessToken=")
        assertFalse(cognitoService.isAuthenticated(cookies))
    }

    @Test
    fun `should return true when JWT is valid in isAuthenticated`() {
        val cookies = listOf("accessToken=validToken")

        val claimsSet = mock<JWTClaimsSet>()
        whenever(claimsSet.claims).thenReturn(mapOf("sub" to "12345"))
        whenever(jwtProcessor.process("validToken", null)).thenReturn(claimsSet)

        assertTrue(cognitoService.isAuthenticated(cookies))

        verify(jwtProcessor).process("validToken", null)
    }

    @Test
    fun `should return false when JWT processing fails in isAuthenticated`() {
        val cookies = listOf("accessToken=invalidToken")

        whenever(jwtProcessor.process("invalidToken", null)).thenThrow(RuntimeException("Invalid token"))

        assertFalse(cognitoService.isAuthenticated(cookies))
        verify(jwtProcessor).process("invalidToken", null)
    }

    @Test
    fun `should authenticate and return JWT when response is successful`() {
        val code = "validCode"
        val response = mock<HttpResponse<String>>()

        whenever(httpClient.send(any(), eq(BodyHandlers.ofString()))).thenReturn(response)
        whenever(response.statusCode()).thenReturn(200)
        whenever(response.body()).thenReturn("""{"access_token":"testAccessToken","expires_in":3600}""")

        val jwt = cognitoService.authenticate(code)

        assertNotNull(jwt)
        assertEquals("testAccessToken", jwt?.accessToken)
        assertEquals(3600, jwt?.expiresIn)
        verify(httpClient).send(any(), eq(BodyHandlers.ofString()))
    }

    @Test
    fun `should return null when authentication response is not 200`() {
        val code = "invalidCode"
        val response = mock<HttpResponse<String>>()

        whenever(httpClient.send(any(), eq(BodyHandlers.ofString()))).thenReturn(response)
        whenever(response.statusCode()).thenReturn(400)

        val jwt = cognitoService.authenticate(code)

        assertNull(jwt)
        verify(httpClient).send(any(), eq(BodyHandlers.ofString()))
    }

    @Test
    fun `should return correct login redirect response`() {
        val response: Response = cognitoService.cognitoLoginRedirectResponse

        assertEquals(302, response.statusCode.statusCode)
        assertTrue(response.path.path.startsWith("https://cognito.testhost.com/login"))
    }
}

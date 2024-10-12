package io.eyecu.passhelper.web

import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.service.Jwt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.thymeleaf.context.Context

class GetLoginRouteTest {

    private val cognitoService = mock<CognitoService>()
    private val domain = "example.com"
    private val loginRoute = GetLoginRoute(cognitoService, domain)

    @Test
    fun `should route correctly`() {
        assertEquals("GET /login", loginRoute.route)
    }

    @Test
    fun `should redirect after handling request`() {
        assertEquals(GetIndex::class, (loginRoute.template as Template.Redirect<*>).redirectRoute)
        assertEquals(302, (loginRoute.template as Template.Redirect<*>).statusCode)
    }

    @Test
    fun `should authenticate and set access token cookie when code is present`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = mapOf("code" to "authCode123"),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        val token = "token123"
        val expires = 3600
        whenever(cognitoService.authenticate("authCode123")).thenReturn(Jwt(token, expires))

        loginRoute.handle(request, context, responseModifier)

        verify(cognitoService).authenticate("authCode123")
        assertTrue(responseModifier.cookies.any { it.contains("accessToken=token123") })
        assertTrue(responseModifier.cookies.any { it.contains("Domain=$domain") })
        assertTrue(responseModifier.cookies.any { it.contains("Max-Age=$expires") })
    }

    @Test
    fun `should do nothing when no code is present in query parameters`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        loginRoute.handle(request, context, responseModifier)

        verify(cognitoService, never()).authenticate(any())
        assertTrue(responseModifier.cookies.isEmpty())
    }

    @Test
    fun `should do nothing if authentication fails`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = mapOf("code" to "authCode123"),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        whenever(cognitoService.authenticate("authCode123")).thenReturn(null)

        loginRoute.handle(request, context, responseModifier)

        verify(cognitoService).authenticate("authCode123")
        assertTrue(responseModifier.cookies.isEmpty())
    }
}

class GetLogoutRouteTest {

    private val domain = "example.com"
    private val logoutRoute = GetLogoutRoute(domain)

    @Test
    fun `should route correctly`() {
        assertEquals("GET /logout", logoutRoute.route)
    }

    @Test
    fun `should redirect after handling request`() {
        assertEquals(GetIndex::class, (logoutRoute.template as Template.Redirect<*>).redirectRoute)
        assertEquals(302, (logoutRoute.template as Template.Redirect<*>).statusCode)
    }

    @Test
    fun `should clear access token cookie`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        logoutRoute.handle(request, context, responseModifier)

        assertTrue(responseModifier.cookies.any { it.contains("accessToken=") })
        assertTrue(responseModifier.cookies.any { it.contains("Domain=$domain") })
        assertTrue(responseModifier.cookies.any { it.contains("Max-Age=1") })
    }
}

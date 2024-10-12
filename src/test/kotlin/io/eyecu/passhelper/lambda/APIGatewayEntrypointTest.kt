package io.eyecu.passhelper.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.web.Body
import io.eyecu.passhelper.web.Path
import io.eyecu.passhelper.web.Request
import io.eyecu.passhelper.web.Response
import io.eyecu.passhelper.web.Route
import io.eyecu.passhelper.web.Router
import io.eyecu.passhelper.web.StatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class APIGatewayEntrypointTest {

    private val cognitoService = mock<CognitoService>()
    private val router = mock<Router>()
    private val entrypoint =
        APIGatewayEntrypoint(apiGatewayServiceProvider = mock(), router = router, cognitoService = cognitoService)

    @Test
    fun `handleRequest should return cognitoLoginRedirectResponse when route requires authentication and user is not authenticated`() {
        val request = createApiGatewayV2HttpEvent(
            routeKey = "GET /protected",
            cookies = listOf("session=invalid")
        )
        val context = mock<Context>()
        val route = mock<Route> {
            on { requiresAuthentication() } doReturn true
        }

        whenever(router.matchingRoute(any())).thenReturn(route)
        whenever(cognitoService.isAuthenticated(any())).thenReturn(false)
        whenever(cognitoService.cognitoLoginRedirectResponse).thenReturn(
            Response(
                statusCode = StatusCode(302),
                path = Path("login"),
                body = Body(null),
                emptyList()
            )
        )

        val response = entrypoint.handleRequest(request, context)

        assertEquals(302, response.statusCode)
        assertEquals("/login", response.headers["location"])
    }

    @Test
    fun `handleRequest should process request when route does not require authentication`() {
        val request = createApiGatewayV2HttpEvent(
            routeKey = "GET /open",
            cookies = listOf("session=valid")
        )
        val context = mock<Context>()
        val route = mock<Route> {
            on { requiresAuthentication() } doReturn false
        }
        val response = Response(
            statusCode = StatusCode(200),
            path = Path("/open"),
            body = Body("response body"),
            cookies = emptyList()
        )
        val apiGatewayResponse = APIGatewayV2HTTPResponse.builder()
            .withStatusCode(200)
            .withBody("response body")
            .build()

        whenever(router.matchingRoute(any())).thenReturn(route)
        whenever(router.processRequest(any(), any())).thenReturn(response)

        val result = entrypoint.handleRequest(request, context)

        assertEquals(apiGatewayResponse.statusCode, result.statusCode)
        assertEquals(apiGatewayResponse.body, result.body)
    }

    @Test
    fun `handleRequest should parse body and pass it to the router`() {
        val request = createApiGatewayV2HttpEvent(
            routeKey = "POST /submit",
            body = "param1=value1&param2=value2"
        )
        val context = mock<Context>()
        val route = mock<Route> {
            on { requiresAuthentication() } doReturn false
        }
        val response = Response(
            statusCode = StatusCode(200),
            path = Path("/submit"),
            body = Body("processed response"),
            cookies = emptyList()
        )

        whenever(router.matchingRoute(any())).thenReturn(route)
        whenever(router.processRequest(any(), any())).thenReturn(response)

        val result = entrypoint.handleRequest(request, context)

        verify(router).processRequest(eq(route), any<Request>())
        assertEquals(200, result.statusCode)
        assertEquals("processed response", result.body)
    }

    @Test
    fun `handleRequest should correctly parse pathParameters and pass them to the router`() {
        val pathParameters = mapOf("id" to "123", "name" to "test")
        val request = createApiGatewayV2HttpEvent(
            routeKey = "GET /resource/{id}",
            pathParameters = pathParameters
        )
        val context = mock<Context>()
        val route = mock<Route> {
            on { requiresAuthentication() } doReturn false
        }
        val response = Response(
            statusCode = StatusCode(200),
            path = Path("/resource/123"),
            body = Body("response with path parameters"),
            cookies = emptyList()
        )

        whenever(router.matchingRoute(any())).thenReturn(route)
        whenever(router.processRequest(any(), any())).thenAnswer {
            val requestArgument = it.getArgument<Request>(1)
            assertEquals(pathParameters, requestArgument.pathParameters)
            response
        }

        val result = entrypoint.handleRequest(request, context)

        verify(router).processRequest(eq(route), any<Request>())
        assertEquals(200, result.statusCode)
        assertEquals("response with path parameters", result.body)
    }

    @Test
    fun `handleRequest should correctly parse queryStringParameters and pass them to the router`() {
        val queryStringParameters = mapOf("param1" to "value1", "param2" to "value2")
        val request = createApiGatewayV2HttpEvent(
            routeKey = "GET /search",
            queryStringParameters = queryStringParameters
        )
        val context = mock<Context>()
        val route = mock<Route> {
            on { requiresAuthentication() } doReturn false
        }
        val response = Response(
            statusCode = StatusCode(200),
            path = Path("/search"),
            body = Body("response with query parameters"),
            cookies = emptyList()
        )

        whenever(router.matchingRoute(any())).thenReturn(route)
        whenever(router.processRequest(any(), any())).thenAnswer {
            val requestArgument = it.getArgument<Request>(1)
            assertEquals(queryStringParameters, requestArgument.queryParameters)
            response
        }

        val result = entrypoint.handleRequest(request, context)

        verify(router).processRequest(eq(route), any<Request>())
        assertEquals(200, result.statusCode)
        assertEquals("response with query parameters", result.body)
    }
}

private fun createApiGatewayV2HttpEvent(
    routeKey: String,
    cookies: List<String> = emptyList(),
    queryStringParameters: Map<String, String>? = null,
    pathParameters: Map<String, String>? = null,
    body: String? = null
) = APIGatewayV2HTTPEvent.builder()
    .withRouteKey(routeKey)
    .withCookies(cookies)
    .withQueryStringParameters(queryStringParameters)
    .withPathParameters(pathParameters)
    .withBody(body)
    .withIsBase64Encoded(false)
    .build()

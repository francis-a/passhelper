package io.eyecu.passhelper.web

import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.service.PassportService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.thymeleaf.context.Context

class GetRootTest {

    private val getRootRoute = GetRoot()

    @Test
    fun `should route correctly`() {
        assertEquals("GET /", getRootRoute.route)
    }

    @Test
    fun `should redirect to index with 301 status code`() {
        assertEquals(GetIndex::class, (getRootRoute.template as Template.Redirect<*>).redirectRoute)
        assertEquals(301, (getRootRoute.template as Template.Redirect<*>).statusCode)
    }

    @Test
    fun `should handle request without modifying context`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        getRootRoute.handle(request, context, responseModifier)

        assertTrue(context.variableNames.isEmpty())
    }
}

class GetIndexTest {

    private val passportService = mock<PassportService>()
    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val getIndexRoute = GetIndex(passportService, notificationEndpointService)

    @Test
    fun `should route correctly`() {
        assertEquals("GET /index", getIndexRoute.route)
    }

    @Test
    fun `should use index template`() {
        assertEquals("index", getIndexRoute.template.templateName)
    }
}

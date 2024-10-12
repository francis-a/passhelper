package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.AddNotificationEndpointForm
import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.util.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.thymeleaf.context.Context

class GetNotificationEndpointsTest {

    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val getNotificationEndpointsRoute = GetNotificationEndpoints(notificationEndpointService)

    @Test
    fun `should route correctly`() {
        assertEquals("GET /notification-endpoints", getNotificationEndpointsRoute.route)
    }

    @Test
    fun `should use notification endpoints template`() {
        assertEquals("notification-endpoints", getNotificationEndpointsRoute.template.templateName)
    }

}

class PostNotificationEndpointsTest {

    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val postNotificationEndpointsRoute = PostNotificationEndpoints(notificationEndpointService)

    @Test
    fun `should route correctly`() {
        assertEquals("POST /notification-endpoints", postNotificationEndpointsRoute.route)
    }

    @Test
    fun `should redirect to index after handling request`() {
        assertEquals(GetIndex::class, (postNotificationEndpointsRoute.template as Template.Redirect<*>).redirectRoute)
    }

    @Test
    fun `should add email to notification service`() {
        val formData = mapOf(
            "email" to "new@example.com"
        )
        val request = Request(
            body = formData,
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        val addEndpointForm = jacksonObjectMapper.convertValue<AddNotificationEndpointForm>(formData)

        postNotificationEndpointsRoute.handle(request, context, responseModifier)

        verify(notificationEndpointService).addEmail(addEndpointForm.email!!)
        assertNotNull(context.getVariable("addNotificationEndpointForm"))
        assertTrue(context.getVariable("addNotificationEndpointForm") is AddNotificationEndpointForm)
    }

}

class DeleteNotificationEndpointTest {

    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val deleteNotificationEndpointRoute = DeleteNotificationEndpoint(notificationEndpointService)

    @Test
    fun `should route correctly`() {
        assertEquals("DELETE /notification-endpoints/{id}", deleteNotificationEndpointRoute.route)
    }

    @Test
    fun `should redirect to notification endpoints after handling request`() {
        assertEquals(
            GetNotificationEndpoints::class,
            (deleteNotificationEndpointRoute.template as Template.Redirect<*>).redirectRoute
        )
    }

    @Test
    fun `should delete notification endpoint using notification service`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = mapOf("id" to "123")
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        deleteNotificationEndpointRoute.handle(request, context, responseModifier)

        verify(notificationEndpointService).deleteEmail("123")
    }
}

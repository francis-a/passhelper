package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.AddUserForm
import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.util.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.thymeleaf.context.Context

class GetUsersTest {

    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val getUsersRoute = GetUsers(mock())

    @Test
    fun `should route correctly`() {
        assertEquals("GET /notification-endpoints", getUsersRoute.route)
    }

    @Test
    fun `should use notification endpoints template`() {
        assertEquals("notification-endpoints", getUsersRoute.template.templateName)
    }

}

class PostUserTest {

    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val postUserRoute = PostUser(mock())

    @Test
    fun `should route correctly`() {
        assertEquals("POST /notification-endpoints", postUserRoute.route)
    }

    @Test
    fun `should redirect to index after handling request`() {
        assertEquals(GetIndex::class, (postUserRoute.template as Template.Redirect<*>).redirectRoute)
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

        val addEndpointForm = jacksonObjectMapper.convertValue<AddUserForm>(formData)

        postUserRoute.handle(request, context, responseModifier)

        verify(notificationEndpointService).addEmail(addEndpointForm.email!!)
        assertNotNull(context.getVariable("addNotificationEndpointForm"))
        assertTrue(context.getVariable("addNotificationEndpointForm") is AddUserForm)
    }

}

class DeleteUserTest {

    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val deleteNotificationEndpointRoute = DeleteUser(mock())

    @Test
    fun `should route correctly`() {
        assertEquals("DELETE /notification-endpoints/{id}", deleteNotificationEndpointRoute.route)
    }

    @Test
    fun `should redirect to notification endpoints after handling request`() {
        assertEquals(
            GetUsers::class,
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

package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.AddUserForm
import io.eyecu.passhelper.service.UserPoolService
import io.eyecu.passhelper.util.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.thymeleaf.context.Context

class GetUsersTest {

    private val getUsersRoute = GetUsers(mock())

    @Test
    fun `should route correctly`() {
        assertEquals("GET /users", getUsersRoute.route)
    }

    @Test
    fun `should use notification endpoints template`() {
        assertEquals("users", getUsersRoute.template.templateName)
    }

}

class PostUserTest {

    private val userPoolService = mock<UserPoolService>()
    private val postUserRoute = PostUser(userPoolService)

    @Test
    fun `should route correctly`() {
        assertEquals("POST /users", postUserRoute.route)
    }

    @Test
    fun `should redirect to user endpoint after handling request`() {
        assertEquals(
            GetUsers::class,
            (postUserRoute.template as Template.Redirect<*>).redirectRoute
        )
    }

    @Test
    fun `should add email to user pool service`() {
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

        verify(userPoolService).createUser(addEndpointForm.email!!)
        assertNotNull(context.getVariable("addNotificationEndpointForm"))
        assertTrue(context.getVariable("addNotificationEndpointForm") is AddUserForm)
    }

}

class DeleteUserTest {

    private val userPoolService = mock<UserPoolService>()
    private val deleteUser = DeleteUser(userPoolService)

    @Test
    fun `should route correctly`() {
        assertEquals("DELETE /users/{username}", deleteUser.route)
    }

    @Test
    fun `should redirect to user endpoint after handling request`() {
        assertEquals(
            GetUsers::class,
            (deleteUser.template as Template.Redirect<*>).redirectRoute
        )
    }

    @Test
    fun `should delete user using user pool service`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = mapOf("username" to "123")
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        deleteUser.handle(request, context, responseModifier)

        verify(userPoolService).deleteUser("123")
    }
}

class PatchUserAttributeValueTest {
    private val userPoolService = mock<UserPoolService>()
    private val patchUser = PatchUserAttributeValue(userPoolService)

    @Test
    fun `should route correctly`() {
        assertEquals("PATCH /users/{username}/attributes/{attribute}/value/{value}", patchUser.route)
    }

    @Test
    fun `should redirect to user endpoint after handling request`() {
        assertEquals(
            GetUsers::class,
            (patchUser.template as Template.Redirect<*>).redirectRoute
        )
    }

    @Test
    fun `should patch user attribute using user pool service`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = mapOf(
                "username" to "123",
                "attribute" to "test",
                "value" to "new value"
            )
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        patchUser.handle(request, context, responseModifier)

        verify(userPoolService).toggleUserAttribute("123", "test", "new value")
    }

}
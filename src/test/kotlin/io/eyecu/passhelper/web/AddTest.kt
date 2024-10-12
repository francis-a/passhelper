package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.CreatePassportForm
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.util.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.thymeleaf.context.Context

class PostAddTest {

    private val passportService = mock<PassportService>()
    private val postAddRoute = PostAdd(passportService)

    @Test
    fun `should route correctly`() {
        assertEquals("POST /add", postAddRoute.route)
    }

    @Test
    fun `should redirect after handling request`() {
        assertEquals(GetIndex::class, (postAddRoute.template as Template.Redirect<*>).redirectRoute)
    }

    @Test
    fun `should save passport form using passport service`() {
        val formData = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "passportNumber" to "123456789"
        )
        val request = Request(
            body = formData,
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context() // Use actual Thymeleaf context
        val responseModifier = ResponseModifier()

        postAddRoute.handle(request, context, responseModifier)

        val expectedForm = jacksonObjectMapper.convertValue<CreatePassportForm>(formData)
        verify(passportService).save(expectedForm)
    }
}

class GetAddTest {

    private val getAddRoute = GetAdd()

    @Test
    fun `should route correctly`() {
        assertEquals("GET /add", getAddRoute.route)
    }

    @Test
    fun `should use add template`() {
        assertEquals("add", getAddRoute.template.templateName)
    }

    @Test
    fun `should add countries and create passport form to context`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context() // Use actual Thymeleaf context
        val responseModifier = ResponseModifier()

        getAddRoute.handle(request, context, responseModifier)

        // Verify that "createPassportForm" is added to the context
        assertNotNull(context.getVariable("createPassportForm"))
        assertTrue(context.getVariable("createPassportForm") is CreatePassportForm)

        // Verify that "countries" is added to the context
        assertNotNull(context.getVariable("countries"))
        assertTrue(context.getVariable("countries") is List<*>)
    }
}

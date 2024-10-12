package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.EditPassportForm
import io.eyecu.passhelper.models.ExpiringWithin
import io.eyecu.passhelper.models.PassportView
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.util.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.thymeleaf.context.Context
import java.time.LocalDate

class GetEditTest {

    private val passportService = mock<PassportService>()
    private val getEditRoute = GetEdit(passportService)

    @Test
    fun `should route correctly`() {
        assertEquals("GET /edit/{id}", getEditRoute.route)
    }

    @Test
    fun `should use edit template`() {
        assertEquals("edit", getEditRoute.template.templateName)
    }

    @Test
    fun `should add edit passport form and passport view to context`() {
        val passportView = PassportView(
            firstName = "John",
            lastName = "Doe",
            number = "123456",
            issuedDate = LocalDate.of(2022, 1, 1),
            expiresDate = LocalDate.of(2032, 1, 1),
            notificationDate = null,
            expiringWithin = ExpiringWithin.HALF_YEAR,
            id = "1",
            dob = LocalDate.of(2022, 1, 1),
            countryCode = "US",
            countryName = "US"
        )
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = mapOf("id" to "123")
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        whenever(passportService.get("123")).thenReturn(passportView)

        getEditRoute.handle(request, context, responseModifier)

        assertNotNull(context.getVariable("editPassportForm"))
        assertTrue(context.getVariable("editPassportForm") is EditPassportForm)

        assertNotNull(context.getVariable("passportView"))
        assertEquals(passportView, context.getVariable("passportView"))
    }
}

class PostEditTest {

    private val passportService = mock<PassportService>()
    private val postEditRoute = PostEdit(passportService)

    @Test
    fun `should route correctly`() {
        assertEquals("POST /edit/{id}", postEditRoute.route)
    }

    @Test
    fun `should redirect after handling request`() {
        assertEquals(GetIndex::class, (postEditRoute.template as Template.Redirect<*>).redirectRoute)
    }

    @Test
    fun `should update passport using passport service`() {
        val formData = mapOf(
            "firstName" to "Jane",
            "lastName" to "Doe",
            "number" to "654321",
            "issuedDate" to "2020-01-01",
            "expiresDate" to "2030-01-01"
        )
        val request = Request(
            body = formData,
            queryParameters = emptyMap(),
            pathParameters = mapOf("id" to "123")
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        val editPassportForm = jacksonObjectMapper.convertValue<EditPassportForm>(formData)

        postEditRoute.handle(request, context, responseModifier)

        verify(passportService).update("123", editPassportForm)
    }
}

class DeleteEditTest {

    private val passportService = mock<PassportService>()
    private val deleteEditRoute = DeleteEdit(passportService)

    @Test
    fun `should route correctly`() {
        assertEquals("DELETE /edit/{id}", deleteEditRoute.route)
    }

    @Test
    fun `should redirect after handling request`() {
        assertEquals(GetIndex::class, (deleteEditRoute.template as Template.Redirect<*>).redirectRoute)
    }

    @Test
    fun `should delete passport using passport service`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = mapOf("id" to "123")
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        deleteEditRoute.handle(request, context, responseModifier)

        verify(passportService).delete("123")
    }
}

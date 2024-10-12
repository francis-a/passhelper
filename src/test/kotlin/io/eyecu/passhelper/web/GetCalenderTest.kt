package io.eyecu.passhelper.web

import io.eyecu.passhelper.service.CalenderService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.thymeleaf.context.Context

class GetCalenderTest {

    private val calenderService = mock<CalenderService>()
    private val getCalenderRoute = GetCalender(calenderService)

    @Test
    fun `should route correctly`() {
        assertEquals("GET /calender", getCalenderRoute.route)
    }

    @Test
    fun `should use calender template`() {
        assertEquals("calender", getCalenderRoute.template.templateName)
    }

    @Test
    fun `should add downloadUrl to context`() {
        val request = Request(
            body = emptyMap(),
            queryParameters = emptyMap(),
            pathParameters = emptyMap()
        )
        val context = Context()
        val responseModifier = ResponseModifier()

        val downloadUrl = "https://example.com/download"
        whenever(calenderService.createPassportExpirationCalender()).thenReturn(downloadUrl)

        getCalenderRoute.handle(request, context, responseModifier)

        assertEquals(downloadUrl, context.getVariable("downloadUrl"))
        verify(calenderService).createPassportExpirationCalender()
    }
}

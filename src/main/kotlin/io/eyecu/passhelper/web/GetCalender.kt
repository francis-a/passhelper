package io.eyecu.passhelper.web

import io.eyecu.passhelper.service.CalenderService
import org.thymeleaf.context.Context

class GetCalender(
    private val calenderService: CalenderService
) : Route {

    override val route = "GET /calender"

    override val template = Template.Static("calender")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val downloadUrl = calenderService.createPassportExpirationCalender()
        context.setVariable("downloadUrl", downloadUrl)
    }
}
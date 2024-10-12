package io.eyecu.passhelper.web

import io.eyecu.passhelper.models.NotificationEndpointView
import io.eyecu.passhelper.models.PassportView
import io.eyecu.passhelper.models.PassportsInYearView
import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.web.Template.Redirect
import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class GetRoot : Route {
    override val route = "GET /"
    override val template = Redirect(GetIndex::class, statusCode = 301)
    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {}
}

class GetIndex(
    private val passportService: PassportService,
    private val notificationEndpointService: NotificationEndpointService
) : Route {
    override val route = "GET /index"
    override val template = Static("index")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val passports = loadAllPassports()
            .groupedByExpirationYear()
            .sortedBy { it.expirationYear }
        val notificationEndpoints = notificationEndpointService.findAllEmails()

        context.addAllPassportsModel(passports)
        context.addNotificationEndpointCount(notificationEndpoints)
        context.addNotificationEndpoints(notificationEndpoints)
    }

    private fun Context.addAllPassportsModel(passportsInYearView: List<PassportsInYearView>) {
        setVariable("passportsInYearView", passportsInYearView)
        setVariable("passportCount", passportsInYearView.size)
    }

    private fun Context.addNotificationEndpoints(notificationEndpoints: List<NotificationEndpointView>) {
        setVariable("notificationEndpoints", notificationEndpoints.joinToString(separator = ", ") { it.email })
    }

    private fun Context.addNotificationEndpointCount(notificationEndpoints: List<NotificationEndpointView>) {
        setVariable("notificationEndpointCount", notificationEndpoints.size)
    }

    private fun loadAllPassports() = passportService.findAll().sortedBy {
        it.expiresDate
    }

    private fun List<PassportView>.groupedByExpirationYear() = groupBy {
        it.expiresDate.year
    }.map { (expirationYear, passports) ->
        PassportsInYearView(expirationYear, passports)
    }
}
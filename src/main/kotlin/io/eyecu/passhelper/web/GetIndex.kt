package io.eyecu.passhelper.web

import io.eyecu.passhelper.models.PassportView
import io.eyecu.passhelper.models.PassportsInYearView
import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.service.UserPoolService
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
    private val userPoolService: UserPoolService
) : Route {
    override val route = "GET /index"
    override val template = Static("index")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val passports = loadAllPassports()
            .groupedByExpirationYear()
            .sortedBy { it.expirationYear }
        val userEmails = userPoolService.listAllUsersWithEmailEnabled()

        context.addAllPassportsModel(passports)
        context.addEmailAddressCount(userEmails)
        context.addUserEmailAddresses(userEmails)
    }

    private fun Context.addAllPassportsModel(passportsInYearView: List<PassportsInYearView>) {
        setVariable("passportsInYearView", passportsInYearView)
        setVariable("passportCount", passportsInYearView.size)
    }

    private fun Context.addUserEmailAddresses(notificationEndpoints: List<UserView>) {
        setVariable("emailAddresses", notificationEndpoints.joinToString(separator = ", ") { it.emailAddress })
    }

    private fun Context.addEmailAddressCount(notificationEndpoints: List<UserView>) {
        setVariable("emailAddressCount", notificationEndpoints.size)
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
package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.EditPassportForm
import io.eyecu.passhelper.models.PassportView
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.util.jacksonObjectMapper
import io.eyecu.passhelper.web.Template.Redirect
import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class GetEdit(
    private val passportService: PassportService
) : Route {
    override val route = "GET /edit/{id}"
    override val template = Static("edit")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val passport = passportService.get(request.passportId())
        context.addEditPassportForm(passport)
        context.addPassportView(passport)
    }

    private fun Context.addEditPassportForm(passport: PassportView) =
        setVariable("editPassportForm", passport.toEditPassportForm())

    private fun Context.addPassportView(passport: PassportView) =
        setVariable("passportView", passport)

    private fun PassportView.toEditPassportForm() =
        EditPassportForm(
            firstName = firstName,
            lastName = lastName,
            number = number,
            issuedDate = issuedDate,
            expiresDate = expiresDate
        )
}

class PostEdit(
    private val passportService: PassportService
) : Route {
    override val route = "POST /edit/{id}"
    override val template = Redirect(GetIndex::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val completedForm = jacksonObjectMapper.convertValue<EditPassportForm>(request.body)
        passportService.update(
            request.passportId(),
            completedForm
        )
    }

}

class DeleteEdit(
    private val passportService: PassportService
) : Route {
    override val route = "DELETE /edit/{id}"
    override val template = Redirect(GetIndex::class)
    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        passportService.delete(request.passportId())
    }
}

private fun Request.passportId() = pathParameters.getValue("id")
package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.CreatePassportForm
import io.eyecu.passhelper.models.countries
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.util.jacksonObjectMapper
import io.eyecu.passhelper.web.Template.Redirect
import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class PostAdd(
    private val passportService: PassportService
) : Route {

    override val route = "POST /add"
    override val template = Redirect(GetIndex::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val completedForm = jacksonObjectMapper.convertValue<CreatePassportForm>(request.body)
        passportService.save(completedForm)
    }

}

class GetAdd : Route {
    override val route = "GET /add"
    override val template = Static("add")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) = with(context) {
        addCountries()
        addCreatePassportForm()
    }

    private fun Context.addCreatePassportForm() = setVariable("createPassportForm", CreatePassportForm())

    private fun Context.addCountries() =
        setVariable("countries", countries)
}


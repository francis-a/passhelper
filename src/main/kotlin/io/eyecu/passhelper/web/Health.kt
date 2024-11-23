package io.eyecu.passhelper.web

import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class GetHealth : Route {

    override val route = "GET /health"
    override val template = Static("health")

    override fun requiresAuthentication() = false

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) = with(context) {
        setVariable("status", "up")
    }
}
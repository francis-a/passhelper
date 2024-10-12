package io.eyecu.passhelper.web

import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.web.Template.Redirect
import io.github.oshai.kotlinlogging.KotlinLogging
import org.thymeleaf.context.Context

class GetLoginRoute(
    private val cognitoService: CognitoService,
    private val domain: String
) : Route {

    private val logger = KotlinLogging.logger {}

    override val route = "GET /login"

    override val template = Redirect(GetIndex::class, statusCode = 302)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        logger.info { "Params: $request" }
        val code = request.queryParameters["code"] ?: return

        val tokenData = cognitoService.authenticate(code) ?: return
        val (token, expires) = tokenData
        responseModifier.setAccessTokenCookie(token, domain, expires)
    }

    override fun requiresAuthentication() = false
}

class GetLogoutRoute(
    private val domain: String
) : Route {

    override val route = "GET /logout"

    override val template = Redirect(GetIndex::class, statusCode = 302)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        responseModifier.setAccessTokenCookie(value = "", domain, maxAge = 1)
    }

    override fun requiresAuthentication() = false
}

private fun ResponseModifier.setAccessTokenCookie(value: String, domain: String, maxAge: Int) {
    val cookie = listOf(
        "accessToken=$value",
        "Domain=$domain",
        "Max-Age=$maxAge",
        "Path=/",
        "Secure",
        "HttpOnly"
    ).joinToString(separator = "; ")
    cookies.add(cookie)
}
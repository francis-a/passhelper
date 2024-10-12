package io.eyecu.passhelper.web

import io.eyecu.passhelper.util.ValueMap
import org.thymeleaf.context.Context
import kotlin.reflect.KClass

interface Route {
    val route: String
    val template: Template

    fun requiresAuthentication(): Boolean = true

    fun handle(request: Request, context: Context, responseModifier: ResponseModifier)
}

sealed class Template {
    data class Static(val templateName: String) : Template()
    data class Redirect<T : Route>(val redirectRoute: KClass<T>, val statusCode: Int = 303) : Template()
}

data class Request(
    val body: ValueMap,
    val queryParameters: ValueMap,
    val pathParameters: ValueMap
)

data class ResponseModifier(
    val cookies: MutableList<String> = mutableListOf(),
)
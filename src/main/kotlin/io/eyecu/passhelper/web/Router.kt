package io.eyecu.passhelper.web

import io.eyecu.passhelper.ApiGatewayServiceProvider
import io.eyecu.passhelper.util.templateEngine
import kotlin.reflect.KClass
import org.thymeleaf.context.Context as ThymeleafContext

typealias RoutesByClass = Map<KClass<out Route>, Route>

data class Response(
    val statusCode: StatusCode,
    val path: Path,
    val body: Body,
    val cookies: List<String>
)

@JvmInline
value class StatusCode(val statusCode: Int)

@JvmInline
value class Path(val path: String)

@JvmInline
value class Body(val body: String?)

class Router(
    apiGatewayServiceProvider: ApiGatewayServiceProvider,
    private val routes: RoutesByClass = routes(apiGatewayServiceProvider)
) {

    companion object {
        private fun routes(apiGatewayServiceProvider: ApiGatewayServiceProvider) = with(apiGatewayServiceProvider) {
            mapOf(
                // auth routes
                GetLoginRoute::class to GetLoginRoute(
                    apiGatewayServiceProvider.cognitoService,
                    apiGatewayServiceProvider.domainName
                ),
                GetLogoutRoute::class to GetLogoutRoute(apiGatewayServiceProvider.domainName),
                // app routes
                GetRoot::class to GetRoot(),
                GetIndex::class to GetIndex(passportService, notificationEndpointService),
                PostAdd::class to PostAdd(passportService),
                GetAdd::class to GetAdd(),
                GetEdit::class to GetEdit(passportService),
                PostEdit::class to PostEdit(passportService),
                DeleteEdit::class to DeleteEdit(passportService),
                GetUsers::class to GetUsers(userPoolService),
                PostUser::class to PostUser(userPoolService),
                DeleteUser::class to DeleteUser(userPoolService),
                PatchUserAttributeValue::class to PatchUserAttributeValue(userPoolService),
                GetCalender::class to GetCalender(calenderService),
                GetHealth::class to GetHealth()
            )
        }
    }

    fun matchingRoute(routeKey: String) = routes.values.first { it.route == routeKey }

    fun processRequest(route: Route, request: Request): Response {
        val thymeleafContext = ThymeleafContext()
        val responseModifier = ResponseModifier()

        route.handle(request, thymeleafContext, responseModifier)

        val (template, statusCode) = route.template()
        val content = templateEngine.process(template, thymeleafContext)

        return Response(
            StatusCode(statusCode),
            Path(template),
            Body(content),
            responseModifier.cookies
        )
    }

    private fun Route.template(statusCode: Int = 200): Pair<String, Int> {
        return when (val t = template) {
            is Template.Redirect<*> -> routes.getValue(t.redirectRoute).template(t.statusCode)
            is Template.Static -> t.templateName to statusCode
        }
    }
}

class DisplayException(override val message: String) : Exception(message)
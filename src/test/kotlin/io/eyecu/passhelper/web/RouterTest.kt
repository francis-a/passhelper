package io.eyecu.passhelper.web

import io.eyecu.passhelper.ApiGatewayServiceProvider
import io.eyecu.passhelper.service.CalenderService
import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.service.PassportService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.thymeleaf.context.Context as ThymeleafContext

class RouterTest {

    private val cognitoService = mock<CognitoService>()
    private val passportService = mock<PassportService>()
    private val notificationEndpointService = mock<NotificationEndpointService>()
    private val calenderService = mock<CalenderService>()
    private val apiGatewayServiceProvider = mock<ApiGatewayServiceProvider> {
        on { cognitoService } doReturn cognitoService
        on { passportService } doReturn passportService
        on { notificationEndpointService } doReturn notificationEndpointService
        on { calenderService } doReturn calenderService
        on { domainName } doReturn "example.com"
    }

    class LoginRoute : Route {
        override val route = "GET /login"
        override val template = Template.Static("login.html")

        override fun handle(request: Request, context: ThymeleafContext, responseModifier: ResponseModifier) {
        }
    }

    class LogoutRoute : Route {
        override val route = "GET /logout"
        override val template = Template.Static("logout.html")

        override fun handle(request: Request, context: ThymeleafContext, responseModifier: ResponseModifier) {
        }
    }

    @Test
    fun `matchingRoute should return correct route based on routeKey`() {
        val customRoutes: RoutesByClass = mapOf(
            LoginRoute::class to LoginRoute(),
            LogoutRoute::class to LogoutRoute()
        )

        val router = Router(apiGatewayServiceProvider, customRoutes)

        val route = router.matchingRoute("GET /login")

        assertTrue(route is LoginRoute)
        assertEquals("GET /login", route.route)
    }


}

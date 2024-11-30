package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.AddNotificationEndpointForm
import io.eyecu.passhelper.models.NotificationEndpointView
import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.service.UserPoolService
import io.eyecu.passhelper.util.jacksonObjectMapper
import io.eyecu.passhelper.web.Template.Redirect
import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class GetNotificationEndpoints(
    private val notificationEndpointService: NotificationEndpointService,
    private val userPoolService: UserPoolService
) : Route {

    override val route = "GET /notification-endpoints"

    override val template = Static("notification-endpoints")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val notificationEndpoints = notificationEndpointService.findAllEmails()
        val users = userPoolService.listAllUsers()
        context.addEndpoints(notificationEndpoints)
        context.addUsers(users)
        context.addNewEndpointForm()
    }

    private fun Context.addEndpoints(notificationEndpoints: List<NotificationEndpointView>) =
        setVariable("notificationEndpoints", notificationEndpoints)

    private fun Context.addUsers(users: List<UserView>) =
        setVariable("users", users)
}

class PostNotificationEndpoints(
    private val notificationEndpointService: NotificationEndpointService,
    private val userPoolService: UserPoolService
) : Route {

    override val route = "POST /notification-endpoints"

    override val template = Redirect(GetNotificationEndpoints::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val addEndpointForm = jacksonObjectMapper.convertValue<AddNotificationEndpointForm>(request.body)
        addEndpointForm.email?.let {
            userPoolService.createUser(it)
        }
    }
}

class DeleteNotificationEndpoint(
    private val notificationEndpointService: NotificationEndpointService,
    private val userPoolService: UserPoolService
) : Route {
    override val route = "DELETE /notification-endpoints/{id}"

    override val template = Redirect(GetNotificationEndpoints::class)
    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val username = request.pathParameters.getValue("id")
        userPoolService.deleteUser(username)
    }
}

class PatchUserAttributeValueEndpoint(
    private val userPoolService: UserPoolService
) : Route {

    override val route = "PATCH /users/{username}/attributes/{attribute}/value/{value}"
    override val template = Redirect(GetNotificationEndpoints::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val username = request.pathParameters.getValue("username")
        val attribute = request.pathParameters.getValue("attribute")
        val value = request.pathParameters.getValue("value").toBooleanStrictOrNull()
            ?: throw DisplayException("Invalid user attribute value ${request.pathParameters.getValue("value")}")

        when (attribute) {
            "email" -> userPoolService.enableOrDisableEmail(username, value)
            "login" -> userPoolService.enableOrDisableUser(username, value)
            else -> throw DisplayException("Invalid attribute $attribute")
        }
    }
}


private fun Context.addNewEndpointForm() =
    setVariable("addNotificationEndpointForm", AddNotificationEndpointForm())
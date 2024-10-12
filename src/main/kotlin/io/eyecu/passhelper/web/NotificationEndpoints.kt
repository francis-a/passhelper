package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.AddNotificationEndpointForm
import io.eyecu.passhelper.models.NotificationEndpointView
import io.eyecu.passhelper.service.NotificationEndpointService
import io.eyecu.passhelper.util.jacksonObjectMapper
import io.eyecu.passhelper.web.Template.Redirect
import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class GetNotificationEndpoints(
    private val notificationEndpointService: NotificationEndpointService
) : Route {

    override val route = "GET /notification-endpoints"

    override val template = Static("notification-endpoints")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val notificationEndpoints = notificationEndpointService.findAllEmails()
        context.addEndpoints(notificationEndpoints)
        context.addNewEndpointForm()
    }

    private fun Context.addEndpoints(notificationEndpoints: List<NotificationEndpointView>) =
        setVariable("notificationEndpoints", notificationEndpoints)
}

class PostNotificationEndpoints(
    private val notificationEndpointService: NotificationEndpointService
) : Route {

    override val route = "POST /notification-endpoints"

    override val template = Redirect(GetIndex::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val addEndpointForm = jacksonObjectMapper.convertValue<AddNotificationEndpointForm>(request.body)
        addEndpointForm.email?.let {
            notificationEndpointService.addEmail(it)
        }
        context.addNewEndpointForm()
    }
}

class DeleteNotificationEndpoint(
    private val notificationEndpointService: NotificationEndpointService
) : Route {
    override val route = "DELETE /notification-endpoints/{id}"

    override val template = Redirect(GetNotificationEndpoints::class)
    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val id = request.pathParameters.getValue("id")
        notificationEndpointService.deleteEmail(id)
    }
}

private fun Context.addNewEndpointForm() =
    setVariable("addNotificationEndpointForm", AddNotificationEndpointForm())
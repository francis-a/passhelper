package io.eyecu.passhelper.web

import com.fasterxml.jackson.module.kotlin.convertValue
import io.eyecu.passhelper.models.AddUserForm
import io.eyecu.passhelper.models.UserView
import io.eyecu.passhelper.service.UserPoolService
import io.eyecu.passhelper.util.jacksonObjectMapper
import io.eyecu.passhelper.web.Template.Redirect
import io.eyecu.passhelper.web.Template.Static
import org.thymeleaf.context.Context

class GetUsers(
    private val userPoolService: UserPoolService
) : Route {

    override val route = "GET /users"

    override val template = Static("users")

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val users = userPoolService.listAllUsers()
        context.addUsers(users)
        context.addNewUserForm()
    }

    private fun Context.addUsers(users: List<UserView>) =
        setVariable("users", users)
}

class PostUser(
    private val userPoolService: UserPoolService
) : Route {

    override val route = "POST /users"

    override val template = Redirect(GetUsers::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val addEndpointForm = jacksonObjectMapper.convertValue<AddUserForm>(request.body)
        addEndpointForm.email?.let {
            userPoolService.createUser(it)
        }
        context.addNewUserForm()
    }
}

class DeleteUser(
    private val userPoolService: UserPoolService
) : Route {
    override val route = "DELETE /users/{username}"

    override val template = Redirect(GetUsers::class)
    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val username = request.pathParameters.getValue("username")
        userPoolService.deleteUser(username)
        context.addNewUserForm()
    }
}

class PatchUserAttributeValue(
    private val userPoolService: UserPoolService
) : Route {

    override val route = "PATCH /users/{username}/attributes/{attribute}/value/{value}"
    override val template = Redirect(GetUsers::class)

    override fun handle(request: Request, context: Context, responseModifier: ResponseModifier) {
        val username = request.pathParameters.getValue("username")
        val attribute = request.pathParameters.getValue("attribute")
        val value = request.pathParameters.getValue("value")

        userPoolService.toggleUserAttribute(username, attribute, value)
        context.addNewUserForm()
    }
}


private fun Context.addNewUserForm() =
    setVariable("addNewUserForm", AddUserForm())
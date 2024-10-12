package io.eyecu.passhelper.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import io.eyecu.passhelper.ApiGatewayServiceProvider
import io.eyecu.passhelper.LambdaApiGatewayServiceProvider
import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.util.QueryParamParser
import io.eyecu.passhelper.util.ValueMap
import io.eyecu.passhelper.web.Path
import io.eyecu.passhelper.web.Request
import io.eyecu.passhelper.web.Response
import io.eyecu.passhelper.web.Route
import io.eyecu.passhelper.web.Router
import io.eyecu.passhelper.web.StatusCode
import java.util.Base64

class APIGatewayEntrypoint(
    apiGatewayServiceProvider: ApiGatewayServiceProvider = LambdaApiGatewayServiceProvider,
    private val router: Router = Router(apiGatewayServiceProvider),
    private val cognitoService: CognitoService = apiGatewayServiceProvider.cognitoService
) : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    override fun handleRequest(request: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val route = router.matchingRoute(request.routeKey)

        return when {
            route.requiresAuthentication() &&
                    !cognitoService.isAuthenticated(request.cookies) ->
                cognitoService.cognitoLoginRedirectResponse

            else -> request.process(route)
        }.toApiGatewayResponse()
    }

    private fun APIGatewayV2HTTPEvent.process(route: Route) = router.processRequest(
        route = route,
        request = Request(
            body = parseBody(),
            queryParameters = queryStringParameters ?: emptyMap(),
            pathParameters = pathParameters ?: emptyMap()
        )
    )

    private fun Response.toApiGatewayResponse(): APIGatewayV2HTTPResponse {
        val (statusCode, path, body) = this

        return APIGatewayV2HTTPResponse.builder()
            .withHeaders(responseHeaders(statusCode, path))
            .withBody(body.body)
            .withIsBase64Encoded(false)
            .withStatusCode(statusCode.statusCode)
            .withCookies(cookies)
            .build()
    }

    private fun responseHeaders(statusCode: StatusCode, path: Path) = with(
        mapOf("content-type" to "text/html")
    ) {
        when (statusCode.statusCode) {
            in 301..307 -> this + mapOf("location" to path.location())
            else -> this
        }
    }

    private fun Path.location() = if (path.startsWith("http")) {
        path
    } else {
        "/$path"
    }

    private fun APIGatewayV2HTTPEvent.parseBody(): ValueMap {
        if (this.body == null) {
            return emptyMap()
        }
        val content = if (isBase64Encoded) {
            Base64.getDecoder().decode(body).decodeToString()
        } else {
            body
        }

        return QueryParamParser.parse(content)
    }
}
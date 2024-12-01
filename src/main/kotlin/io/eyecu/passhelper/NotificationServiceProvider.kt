package io.eyecu.passhelper

import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.service.NotificationService
import io.eyecu.passhelper.service.UserPoolService
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.ses.SesClient
import java.lang.System.getenv

interface NotificationServiceProvider {
    val notificationService: NotificationService
}

object LambdaNotificationEndpointServiceServiceProvider : NotificationServiceProvider {
    private val cognitoUserPoolId: String = getenv("COGNITO_USER_POOL_ID")
    private val passportTableName = System.getenv("PASSPORT_TABLE_NAME")
    private val emailDomain = System.getenv("EMAIL_DOMAIN")
    private val emailName = System.getenv("EMAIL_NAME")

    private val dynamoDbClient: DynamoDbEnhancedClient = DynamoDbEnhancedClient
        .builder()
        .dynamoDbClient(DynamoDbClient.builder().build())
        .build()

    private val sesClient: SesClient = SesClient.builder().build()

    override val notificationService = NotificationService(
        sesClient = sesClient,
        userPoolService = UserPoolService(
            CognitoIdentityProviderClient.builder().build(),
            cognitoUserPoolId
        ),
        passportRepository = PassportRepository(
            tableName = passportTableName,
            client = dynamoDbClient
        ),
        domain = emailDomain,
        emailName = emailName
    )
}
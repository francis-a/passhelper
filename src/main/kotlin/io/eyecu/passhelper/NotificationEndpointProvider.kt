package io.eyecu.passhelper

import io.eyecu.passhelper.repository.NotificationEndpointRepository
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.service.NotificationService
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.ses.SesClient

interface NotificationEndpointServiceProvider {
    val notificationService: NotificationService
}

object LambdaNotificationEndpointServiceProvider : NotificationEndpointServiceProvider {
    private val notificationEndpointTableName = System.getenv("NOTIFICATION_ENDPOINT_TABLE_NAME")
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
        notificationEndpointRepository = NotificationEndpointRepository(
            tableName = notificationEndpointTableName,
            client = dynamoDbClient
        ),
        passportRepository = PassportRepository(
            tableName = passportTableName,
            client = dynamoDbClient
        ),
        domain = emailDomain,
        emailName = emailName
    )
}
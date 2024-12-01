package io.eyecu.passhelper

import io.eyecu.passhelper.repository.PassportNotificationRepository
import io.eyecu.passhelper.repository.PassportRepository
import io.eyecu.passhelper.service.CalenderService
import io.eyecu.passhelper.service.CognitoService
import io.eyecu.passhelper.service.EmailService
import io.eyecu.passhelper.service.PassportService
import io.eyecu.passhelper.service.UserPoolService
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ses.SesClient
import java.lang.System.getenv

interface ApiGatewayServiceProvider {
    val passportService: PassportService
    val userPoolService: UserPoolService
    val cognitoService: CognitoService
    val calenderService: CalenderService
    val domainName: String
}

object LambdaApiGatewayServiceProvider : ApiGatewayServiceProvider {

    private val passportTableName: String = getenv("PASSPORT_TABLE_NAME")
    private val notificationTableName: String = getenv("PASSPORT_NOTIFICATIONS_TABLE_NAME")
    private val cognitoClientId: String = getenv("COGNITO_CLIENT_ID")
    private val cognitoClientSecret: String = getenv("COGNITO_CLIENT_SECRET")
    private val cognitoUserPoolId: String = getenv("COGNITO_USER_POOL_ID")
    private val awsRegion: String = getenv("REGION")
    private val icsBucket = getenv("ICS_BUCKET_NAME")
    private val cognitoHostedUiUrl = getenv("COGNITO_HOSTED_UI_URL")

    override val domainName: String = getenv("DOMAIN_NAME")

    private val dynamodbClient: DynamoDbEnhancedClient = DynamoDbEnhancedClient
        .builder()
        .dynamoDbClient(DynamoDbClient.builder().build())
        .build()

    override val passportService =
        PassportService(
            PassportRepository(passportTableName, dynamodbClient),
            PassportNotificationRepository(notificationTableName, dynamodbClient)
        )

    override val cognitoService =
        CognitoService(
            awsRegion = awsRegion,
            userPoolId = cognitoUserPoolId,
            clientId = cognitoClientId,
            clientSecret = cognitoClientSecret,
            cognitoHostedUiUrl = cognitoHostedUiUrl,
            domainName = domainName,
        )

    override val calenderService =
        CalenderService(
            icsBucket = icsBucket,
            passportService = passportService,
            awsS3 = S3Client.builder().build()
        )

    override val userPoolService =
        UserPoolService(
            emailService = EmailService(
                sesClient = SesClient.create(),
            ),
            cognitoClient = CognitoIdentityProviderClient.builder().build(),
            userPoolId = cognitoUserPoolId,
            domainName = domainName
        )
}
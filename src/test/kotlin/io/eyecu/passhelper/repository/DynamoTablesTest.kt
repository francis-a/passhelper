package io.eyecu.passhelper.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@Testcontainers
abstract class DynamoTablesTest(
    private val schema: BeanTableSchema<*>,
    internal val tableName: String
) {
    private val client =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(
                DynamoDbClient.builder()
                    .region(Region.of(localstackContainer.region))
                    .credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                localstackContainer.accessKey,
                                localstackContainer.secretKey
                            )
                        )
                    )
                    .endpointOverride(
                        localstackContainer.getEndpointOverride(DYNAMODB)
                    )
                    .build()
            )
            .build()

    @BeforeEach
    fun before() {
        client.table(tableName, schema).createTable()
        createRepository(client)
    }

    @AfterEach
    fun after() {
        client.table(tableName, schema).deleteTable()
    }

    abstract fun createRepository(client: DynamoDbEnhancedClient)

    companion object {

        @Container
        private val localstackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(
                DYNAMODB
            )


    }
}
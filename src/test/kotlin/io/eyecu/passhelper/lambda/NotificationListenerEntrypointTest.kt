package io.eyecu.passhelper.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Identity
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord
import io.eyecu.passhelper.NotificationEndpointServiceProvider
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.SortKey
import io.eyecu.passhelper.service.NotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.mock

class NotificationListenerEntrypointTest {

    private val notificationService = mock<NotificationService>()

    private val context = mock<Context>()

    private lateinit var service: NotificationListenerEntrypoint

    @BeforeEach
    fun before() {
        service = NotificationListenerEntrypoint(object : NotificationEndpointServiceProvider {
            override val notificationService =
                this@NotificationListenerEntrypointTest.notificationService
        })
    }

    @Test
    fun `should process stream record`() {
        service.handleRequest(createDynamoDbEvent(), context)

        verify(notificationService).send(PartitionKey("name"), SortKey("identifier"))
    }

    @Test
    fun `should extract hash and short keys`() {
        service.handleRequest(createDynamoDbEvent(name = "testName", identifier = "testIdentifier"), context)

        verify(notificationService).send(PartitionKey("testName"), SortKey("testIdentifier"))
    }

    @Test
    fun `should skip event if it is not remove event`() {
        service.handleRequest(createDynamoDbEvent(eventName = "INSERT"), context)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if it is not ttl triggered`() {
        service.handleRequest(createDynamoDbEvent(identityType = "User"), context)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if principal id is not the same`() {
        service.handleRequest(
            createDynamoDbEvent(identityPrincipalId = "different"),
            context
        )

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if it does not contain name key`() {
        val event = createDynamoDbEvent()
        event.records.first().dynamodb.keys.remove("name")

        service.handleRequest(event, context)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if it does not contain identifier key`() {
        val event = createDynamoDbEvent()
        event.records.first().dynamodb.keys.remove("identifier")

        service.handleRequest(event, context)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if dynamodb record is null`() {
        val event = createDynamoDbEvent()
        event.records.first().dynamodb = null

        service.handleRequest(event, context)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if stream record is null`() {
        val event = createDynamoDbEvent()
        event.records = listOf(null)

        service.handleRequest(event, context)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `should skip event if stream record list is empty`() {
        val event = createDynamoDbEvent()
        event.records = listOf()

        service.handleRequest(event, context)

        verifyNoInteractions(notificationService)
    }

    private fun createDynamoDbEvent(
        name: String = "name",
        identifier: String = "identifier",
        eventName: String = "REMOVE",
        identityType: String = "Service",
        identityPrincipalId: String = "dynamodb.amazonaws.com"
    ) = DynamodbEvent().apply {
        records = listOf(
            DynamodbStreamRecord().apply {
                dynamodb = StreamRecord().apply {
                    keys = mapOf(
                        "name" to AttributeValue().apply {
                            s = name
                        },
                        "identifier" to AttributeValue().apply {
                            s = identifier
                        }
                    )
                }
                this.eventName = eventName
                userIdentity = Identity().apply {
                    type = identityType
                    principalId = identityPrincipalId
                }
            }
        )
    }


}
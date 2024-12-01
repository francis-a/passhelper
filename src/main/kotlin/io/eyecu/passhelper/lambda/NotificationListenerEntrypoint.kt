package io.eyecu.passhelper.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord
import io.eyecu.passhelper.LambdaNotificationEndpointServiceServiceProvider
import io.eyecu.passhelper.NotificationServiceProvider
import io.eyecu.passhelper.repository.PartitionKey
import io.eyecu.passhelper.repository.SortKey

class NotificationListenerEntrypoint(
    notificationEndpointServiceProvider: NotificationServiceProvider = LambdaNotificationEndpointServiceServiceProvider
) : RequestHandler<DynamodbEvent, String> {

    private val notificationService = notificationEndpointServiceProvider.notificationService

    override fun handleRequest(dynamoEvent: DynamodbEvent, context: Context): String {
        dynamoEvent.records?.filter {
            it.isNotNull() &&
                    it.containsCorrectKeys() &&
                    it.isRemoveEvent() &&
                    it.isTtlTriggered()
        }?.forEach {
            val (partition, sort) = it.extractKey()
            notificationService.send(partition, sort)
        }
        return "OK"
    }

    private fun DynamodbStreamRecord?.isNotNull() =
        this != null && this.dynamodb != null

    private fun DynamodbStreamRecord.containsCorrectKeys() =
        dynamodb.keys.containsKey("name") &&
                dynamodb.keys.containsKey("identifier")

    private fun DynamodbStreamRecord.isRemoveEvent() =
        eventName == "REMOVE"

    private fun DynamodbStreamRecord.isTtlTriggered() =
        userIdentity?.type?.equals("Service", ignoreCase = true) == true &&
                userIdentity?.principalId?.equals("dynamodb.amazonaws.com", ignoreCase = true) == true

    private fun DynamodbStreamRecord.extractKey(): Pair<PartitionKey, SortKey> {
        val hash = this.dynamodb.keys.getValue("name").s
        val range = this.dynamodb.keys.getValue("identifier").s
        return PartitionKey(hash) to SortKey(range)
    }

}
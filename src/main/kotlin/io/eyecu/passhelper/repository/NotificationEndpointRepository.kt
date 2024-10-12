package io.eyecu.passhelper.repository

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

class NotificationEndpointRepository(
    tableName: String,
    client: DynamoDbEnhancedClient
) {

    private val table = client.table(
        tableName,
        StaticTableSchema.builder(NotificationEndpoint::class.java)
            .newItemSupplier { NotificationEndpoint() }
            .addAttribute(NotificationEndpoint::type.attribute(primaryPartitionKey()))
            .addAttribute(NotificationEndpoint::value.attribute(primarySortKey()))
            .build()
    )

    fun findAllEmails(): List<String> = table.query(
        QueryConditional.keyEqualTo(
            Key.builder()
                .partitionValue("email")
                .build()
        )
    ).items().map {
        it.value
    }

    fun addEmail(value: String) =
        table.putItem(
            NotificationEndpoint(
                type = "email",
                value = value.lowercase()
            )
        )

    fun deleteEmail(value: String) {
        table.deleteItem(
            NotificationEndpoint(
                type = "email",
                value = value.lowercase()
            )
        )
    }

}

@DynamoDbBean
data class NotificationEndpoint(
    @get:DynamoDbPartitionKey
    var type: String = "",
    @get:DynamoDbSortKey
    var value: String = ""
)
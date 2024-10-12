package io.eyecu.passhelper.repository

import io.eyecu.passhelper.util.secondsToLocalDate
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import java.time.LocalDate
import java.time.LocalTime.MIDNIGHT
import java.time.ZoneOffset.UTC

class PassportNotificationRepository(
    tableName: String,
    client: DynamoDbEnhancedClient
) {

    private val table = client.table(
        tableName,
        StaticTableSchema
            .builder(PassportNotification::class.java)
            .newItemSupplier { PassportNotification() }
            .addAttribute(PassportNotification::name.attribute(primaryPartitionKey()))
            .addAttribute(PassportNotification::identifier.attribute(primarySortKey()))
            .addAttribute(PassportNotification::expires.attribute())
            .build()
    )

    fun put(passportId: String, notificationTime: LocalDate) =
        with(dynamoPartitionAndSortKey(passportId)) {
            val (partition, sort) = this
            table.putItem(
                PassportNotification(
                    name = partition.hash,
                    identifier = sort.range,
                    expires = notificationTime.toEpochSecond(MIDNIGHT, UTC)
                )
            )
        }

    fun get(passportId: String): LocalDate? =
        with(dynamoPartitionAndSortKey(passportId)) {
            val (partition, sort) = this
            val notification = table.getItem(
                Key.builder()
                    .partitionValue(partition.hash)
                    .sortValue(sort.range)
                    .build()
            )

            return notification?.expires?.let {
                localDateFromSeconds(it)
            }
        }

    fun findAll(): Map<String, LocalDate> = table.scan().mapNotificationDates()

    fun findAllMatching(passportId: String): Map<String, LocalDate> = with(dynamoPartitionAndSortKey(passportId)) {
        val (partition, _) = this
        table.query(
            QueryConditional.keyEqualTo(
                Key.builder()
                    .partitionValue(partition.hash)
                    .build()
            )
        ).mapNotificationDates()
    }

    private fun PageIterable<PassportNotification>.mapNotificationDates() =
        items().mapNotNull {
            val passportId = it.passportId()
            val expires = it.expires ?: return@mapNotNull null
            passportId to localDateFromSeconds(expires)
        }.toMap()

    fun delete(passportId: String): PassportNotification =
        with(dynamoPartitionAndSortKey(passportId)) {
            val (partition, sort) = this
            // remove the TTL before delete to avoid sending a notification
            val key = PassportNotification(partition.hash, sort.range)
            table.putItem(key)
            table.deleteItem(
                Key.builder()
                    .partitionValue(partition.hash)
                    .sortValue(sort.range)
                    .build()
            )
        }

    private fun localDateFromSeconds(seconds: Long) = secondsToLocalDate(seconds)

}

@DynamoDbBean
data class PassportNotification(
    @get:DynamoDbPartitionKey
    override var name: String = "",
    @get:DynamoDbSortKey
    override var identifier: String = "",
    @get:DynamoDbAttribute("expires")
    var expires: Long? = null,
) : PassportId
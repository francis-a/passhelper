package io.eyecu.passhelper.repository

import io.eyecu.passhelper.web.DisplayException
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

class PassportRepository(
    tableName: String,
    private val client: DynamoDbEnhancedClient
) {

    private val table =
        client.table(
            tableName,
            StaticTableSchema
                .builder(DynamoPassport::class.java)
                .newItemSupplier { DynamoPassport() }
                .addAttribute(DynamoPassport::name.attribute(primaryPartitionKey()))
                .addAttribute(DynamoPassport::identifier.attribute(primarySortKey()))
                .addAttribute(DynamoPassport::number.attribute())
                .addAttribute(DynamoPassport::countryCode.attribute())
                .addAttribute(DynamoPassport::firstName.attribute())
                .addAttribute(DynamoPassport::lastName.attribute())
                .addAttribute(DynamoPassport::dob.attribute())
                .addAttribute(DynamoPassport::issued.attribute())
                .addAttribute(DynamoPassport::expires.attribute())
                .build()
        )

    fun save(createForm: CreatePassportRequest): String {
        val passport = createForm.toDynamo()
        val existing = table.getItem(passport)
        if (existing != null) {
            throw DisplayException("This passport already exists")
        }
        table.putItem(passport)
        return passport.passportId()
    }

    fun update(id: String, updateForm: EditPassportRequest): String {
        val oldPassport = getInternal(id)
        val updatedPassport = updateForm.mergeWith(oldPassport)
        // key changed, need to recreate the item
        if (oldPassport.passportId() != updatedPassport.passportId()) {
            client.transactWriteItems {
                it.addDeleteItem(
                    table,
                    Key.builder()
                        .partitionValue(oldPassport.name)
                        .sortValue(oldPassport.identifier)
                        .build()
                )
                it.addPutItem(table, updatedPassport)
            }
        } else {
            table.updateItem(updatedPassport)
        }
        return updatedPassport.passportId()
    }

    fun find(partitionKey: PartitionKey, sortKey: SortKey) = table
        .getItem(
            Key.builder()
                .partitionValue(partitionKey.hash)
                .sortValue(sortKey.range)
                .build()
        )?.fromDynamo()

    fun get(id: String): Passport = getInternal(id).fromDynamo()

    private fun getInternal(id: String): DynamoPassport = with(dynamoPartitionAndSortKey(id)) {
        val (partition, sort) = this
        table
            .getItem(
                Key.builder()
                    .partitionValue(partition.hash)
                    .sortValue(sort.range)
                    .build()
            )
    } ?: throw DisplayException("Passport not found")

    fun delete(id: String): Passport? = with(dynamoPartitionAndSortKey(id)) {
        val (partition, sort) = this
        table
            .deleteItem(
                Key.builder()
                    .partitionValue(partition.hash)
                    .sortValue(sort.range)
                    .build()
            )?.fromDynamo()
    }

    fun findAll(): Iterable<Passport> = table
        .scan()
        .items().map { it.fromDynamo() }

    fun findAllMatching(id: String): List<Passport> = with(dynamoPartitionAndSortKey(id)) {
        val (partition, _) = this
        table.query(
            QueryConditional.keyEqualTo(
                Key.builder()
                    .partitionValue(partition.hash)
                    .build()
            )
        ).items().map {
            it.fromDynamo()
        }
    }

    private fun DynamoPassport.fromDynamo() = Passport(
        id = passportId(),
        firstName = firstName,
        lastName = lastName,
        dob = dob,
        number = number,
        countryCode = countryCode,
        issued = issued,
        expires = expires
    )

    private fun CreatePassportRequest.toDynamo() = DynamoPassport(
        name = partitionKey(firstName = firstName, lastName = lastName),
        identifier = sortKey(
            countryCode = countryCode,
            number = number
        ),
        number = number,
        countryCode = countryCode,
        expires = expires,
        issued = issued,
        firstName = firstName,
        lastName = lastName,
        dob = dob
    )

    private fun EditPassportRequest.mergeWith(existing: DynamoPassport) = DynamoPassport(
        name = partitionKey(firstName = firstName, lastName = lastName),
        identifier = sortKey(countryCode = existing.countryCode, number = number),
        countryCode = existing.countryCode,
        dob = existing.dob,
        firstName = firstName,
        lastName = lastName,
        number = number,
        issued = issued,
        expires = expires
    )

    private fun partitionKey(firstName: String, lastName: String) =
        "$firstName^$lastName".lowercase()

    private fun sortKey(countryCode: String, number: String) =
        "$countryCode^$number".lowercase()

    data class CreatePassportRequest(
        val firstName: String,
        val lastName: String,
        val dob: Long,
        val number: String,
        val countryCode: String,
        val issued: Long,
        val expires: Long,
    )

    data class EditPassportRequest(
        val firstName: String,
        val lastName: String,
        val number: String,
        val issued: Long,
        val expires: Long,
    )

    data class Passport(
        val id: String,
        val firstName: String,
        val lastName: String,
        val dob: Long,
        val number: String,
        val countryCode: String,
        val issued: Long,
        val expires: Long
    )
}

@DynamoDbBean
data class DynamoPassport(
    @get:DynamoDbPartitionKey
    override var name: String = "",
    @get:DynamoDbSortKey
    override var identifier: String = "",
    @get:DynamoDbAttribute(value = "number")
    var number: String = "",
    @get:DynamoDbAttribute(value = "countryCode")
    var countryCode: String = "",
    @get:DynamoDbAttribute(value = "firstName")
    var firstName: String = "",
    @get:DynamoDbAttribute(value = "lastName")
    var lastName: String = "",
    @get:DynamoDbAttribute(value = "dob")
    var dob: Long = 0,
    @get:DynamoDbAttribute(value = "issued")
    var issued: Long = 0,
    @get:DynamoDbAttribute(value = "long")
    var expires: Long = 0
) : PassportId
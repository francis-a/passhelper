package io.eyecu.passhelper.repository

import io.eyecu.passhelper.repository.PassportRepository.CreatePassportRequest
import io.eyecu.passhelper.repository.PassportRepository.EditPassportRequest
import io.eyecu.passhelper.web.DisplayException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

@Testcontainers
class PassportRepositoryTest : DynamoTablesTest(
    schema = TableSchema.fromBean(DynamoPassport::class.java),
    tableName = "passport_test"
) {
    private val savePassport = CreatePassportRequest(
        firstName = "test",
        lastName = "test er",
        number = "1234",
        countryCode = "CA",
        issued = 1,
        expires = 2,
        dob = 4
    )

    private lateinit var passportRepository: PassportRepository

    override fun createRepository(client: DynamoDbEnhancedClient) {
        passportRepository = PassportRepository(tableName, client)
    }

    @Test
    fun `should save item`() {
        val passportId = passportRepository.save(savePassport)
        val result = passportRepository.get(passportId)

        assertThat(result).isNotNull
        assertThat(result.firstName).isEqualTo(savePassport.firstName)
        assertThat(result.lastName).isEqualTo(savePassport.lastName)
        assertThat(result.number).isEqualTo(savePassport.number)
        assertThat(result.countryCode).isEqualTo(savePassport.countryCode)
        assertThat(result.issued).isEqualTo(savePassport.issued)
        assertThat(result.expires).isEqualTo(savePassport.expires)
        assertThat(result.dob).isEqualTo(savePassport.dob)
    }

    @Test
    fun `should load multiple items`() {
        val passport2 = CreatePassportRequest(
            firstName = "test 12",
            lastName = "test er 12",
            number = "1234 12",
            countryCode = "it",
            issued = 60,
            expires = 70,
            dob = 80
        )
        passportRepository.save(savePassport)
        passportRepository.save(passport2)
        val results = passportRepository.findAll()

        assertThat(results.toList()).hasSize(2)
    }

    @Test
    fun `should throw if no passport found`() {
        assertThrows<DisplayException> {
            passportRepository.get("YV5iI2NeZA==")
        }
    }

    @Test
    fun `should return empty list if no passports exist`() {
        assertThat(passportRepository.findAll()).isEmpty()
    }

    @Test
    fun `should delete passport`() {
        val id = passportRepository.save(savePassport)
        passportRepository.delete(id)

        assertThrows<DisplayException> {
            passportRepository.get(id)
        }
    }

    @Test
    fun `should return null when deleting a passport that doesnt exist`() {
        assertThat(passportRepository.delete("YV5iI2NeZA==")).isNull()
    }

    @Test
    fun `should update passport name changes`() = runPassportUpdateTest(
        EditPassportRequest(
            firstName = "new new",
            lastName = "new new",
            number = savePassport.number,
            issued = savePassport.issued,
            expires = savePassport.expires
        )
    )

    @Test
    fun `should update passport if dates changes`() = runPassportUpdateTest(
        EditPassportRequest(
            firstName = savePassport.firstName,
            lastName = savePassport.lastName,
            number = savePassport.number,
            issued = 22,
            expires = 44
        )
    )

    @Test
    fun `should update passport if number changes`() = runPassportUpdateTest(
        EditPassportRequest(
            firstName = savePassport.firstName,
            lastName = savePassport.lastName,
            number = "new",
            issued = savePassport.issued,
            expires = savePassport.expires
        )
    )

    @Test
    fun `should throw if id is invalid`() {
        assertThrows<DisplayException> {
            passportRepository.get("invalid id")
        }
    }

    @Test
    fun `should find all matching partial id`() {
        val id1 = passportRepository.save(savePassport)
        val id2 = passportRepository.save(
            savePassport.copy(
                number = "changed number"
            )
        )

        val firstResults = passportRepository.findAllMatching(id1)
        assertThat(firstResults).hasSize(2)

        val secondResults = passportRepository.findAllMatching(id2)
        assertThat(secondResults).hasSize(2)

        assertThat(firstResults).containsExactlyInAnyOrder(*secondResults.toTypedArray())
    }

    private fun runPassportUpdateTest(updatedPassport: EditPassportRequest) {
        val id = passportRepository.save(savePassport)
        val updatedId = passportRepository.update(id, updatedPassport)
        val result = passportRepository.get(updatedId)

        assertThat(result).isNotNull
        assertThat(result.firstName).isEqualTo(updatedPassport.firstName)
        assertThat(result.lastName).isEqualTo(updatedPassport.lastName)
        assertThat(result.number).isEqualTo(updatedPassport.number)
        assertThat(result.issued).isEqualTo(updatedPassport.issued)
        assertThat(result.expires).isEqualTo(updatedPassport.expires)

        val allPassports = passportRepository.findAll().toList()
        assertThat(allPassports).hasSize(1)
    }
}